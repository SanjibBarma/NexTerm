#include <jni.h>
#include <string>
#include <vector>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <pty.h>
#include <signal.h>
#include <errno.h>
#include <cstring>
#include <android/log.h>

#define LOG_TAG "NexTermPTY"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static jclass gPtyProcessClass = nullptr;
static jmethodID gPtyProcessCtor = nullptr;

struct PtyHandle {
    int masterFd = -1;
    pid_t pid = -1;
};

static std::string jstringToString(JNIEnv* env, jstring str) {
    if (!str) return "";
    const char* chars = env->GetStringUTFChars(str, nullptr);
    std::string result = chars ? chars : "";
    if (chars) env->ReleaseStringUTFChars(str, chars);
    return result;
}

static std::vector<std::string> jobjectArrayToVector(JNIEnv* env, jobjectArray array) {
    std::vector<std::string> result;
    if (!array) return result;

    jsize len = env->GetArrayLength(array);
    result.reserve(len);

    for (jsize i = 0; i < len; ++i) {
        auto item = (jstring) env->GetObjectArrayElement(array, i);
        result.push_back(jstringToString(env, item));
        env->DeleteLocalRef(item);
    }
    return result;
}

static std::vector<char*> toCharPtrArray(std::vector<std::string>& strings) {
    std::vector<char*> out;
    out.reserve(strings.size() + 1);
    for (auto& s : strings) {
        out.push_back(s.data());
    }
    out.push_back(nullptr);
    return out;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nexterm_app_terminal_pty_NativePty_nativeCreateProcess(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring shell_,
        jobjectArray args_,
        jobjectArray envVars_,
        jstring cwd_,
        jint rows,
        jint cols
) {
    std::string shell = jstringToString(env, shell_);
    std::string cwd = jstringToString(env, cwd_);

    auto argsVec = jobjectArrayToVector(env, args_);
    auto envVec = jobjectArrayToVector(env, envVars_);

    if (shell.empty()) {
        shell = "/system/bin/sh";
    }

    if (argsVec.empty()) {
        argsVec.push_back(shell);
        argsVec.push_back("-");
    } else if (argsVec[0].empty()) {
        argsVec[0] = shell;
    }

    struct winsize ws{};
    ws.ws_row = static_cast<unsigned short>(rows > 0 ? rows : 24);
    ws.ws_col = static_cast<unsigned short>(cols > 0 ? cols : 80);
    ws.ws_xpixel = 0;
    ws.ws_ypixel = 0;

    int masterFd = -1;
    pid_t childPid = forkpty(&masterFd, nullptr, nullptr, &ws);
    if (childPid < 0) {
        LOGE("forkpty failed: %s", strerror(errno));
        return nullptr;
    }

    if (childPid == 0) {
        if (!cwd.empty()) {
            chdir(cwd.c_str());
        }

        std::vector<std::string> argvStrings = argsVec;
        if (argvStrings.empty()) {
            argvStrings.push_back(shell);
            argvStrings.push_back("-");
        }

        auto argv = toCharPtrArray(argvStrings);

        std::vector<std::string> envStrings = envVec;
        auto envp = toCharPtrArray(envStrings);

        execve(shell.c_str(), argv.data(), envp.data());
        _exit(127);
    }

    jobject processObj = env->NewObject(
            gPtyProcessClass,
            gPtyProcessCtor,
            static_cast<jint>(masterFd),
            static_cast<jint>(childPid)
    );

    return processObj;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_nexterm_app_terminal_pty_NativePty_nativeRead(
        JNIEnv* env,
        jobject /*thiz*/,
        jint fd,
        jbyteArray buffer_,
        jint offset,
        jint length
) {
    if (fd < 0 || !buffer_ || length <= 0) return -1;

    jbyte* buffer = env->GetByteArrayElements(buffer_, nullptr);
    if (!buffer) return -1;

    ssize_t count = read(fd, buffer + offset, length);
    env->ReleaseByteArrayElements(buffer_, buffer, 0);

    if (count < 0) {
        if (errno == EINTR) return 0;
        return -1;
    }

    return static_cast<jint>(count);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_nexterm_app_terminal_pty_NativePty_nativeWrite(
        JNIEnv* env,
        jobject /*thiz*/,
        jint fd,
        jbyteArray data_,
        jint offset,
        jint length
) {
    if (fd < 0 || !data_ || length <= 0) return -1;

    jbyte* data = env->GetByteArrayElements(data_, nullptr);
    if (!data) return -1;

    ssize_t written = write(fd, data + offset, length);
    env->ReleaseByteArrayElements(data_, data, JNI_ABORT);

    if (written < 0) {
        if (errno == EINTR) return 0;
        return -1;
    }

    return static_cast<jint>(written);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nexterm_app_terminal_pty_NativePty_nativeResize(
        JNIEnv* /*env*/,
jobject /*thiz*/,
jint fd,
        jint rows,
jint cols
) {
if (fd < 0) return;

struct winsize ws{};
ws.ws_row = static_cast<unsigned short>(rows > 0 ? rows : 24);
ws.ws_col = static_cast<unsigned short>(cols > 0 ? cols : 80);
ws.ws_xpixel = 0;
ws.ws_ypixel = 0;

ioctl(fd, TIOCSWINSZ, &ws);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nexterm_app_terminal_pty_NativePty_nativeClose(
        JNIEnv* /*env*/,
jobject /*thiz*/,
jint fd
) {
if (fd >= 0) {
close(fd);
}
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nexterm_app_terminal_pty_NativePty_nativeHangupProcess(
        JNIEnv* /*env*/,
jobject /*thiz*/,
jint pid
) {
if (pid > 0) {
kill(pid, SIGHUP);
}
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nexterm_app_terminal_pty_NativePty_nativeInterruptProcess(
        JNIEnv* /*env*/,
jobject /*thiz*/,
jint pid
) {
if (pid > 0) {
kill(pid, SIGINT);
}
}

extern "C"
JNIEXPORT jboolean JNICALL
        Java_com_nexterm_app_terminal_pty_NativePty_nativeIsAlive(
        JNIEnv* /*env*/,
        jobject /*thiz*/,
        jint pid
) {
if (pid <= 0) return JNI_FALSE;
if (kill(pid, 0) == 0) return JNI_TRUE;
return JNI_FALSE;
}

extern "C"
JNIEXPORT jint JNICALL
        Java_com_nexterm_app_terminal_pty_NativePty_nativeWaitFor(
        JNIEnv* /*env*/,
        jobject /*thiz*/,
        jint pid
) {
if (pid <= 0) return -1;

int status = 0;
pid_t result = waitpid(pid, &status, 0);
if (result < 0) return -1;

if (WIFEXITED(status)) return WEXITSTATUS(status);
if (WIFSIGNALED(status)) return 128 + WTERMSIG(status);
return -1;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass localClass = env->FindClass("com/nexterm/app/terminal/pty/PtyProcess");
    if (!localClass) return JNI_ERR;

    gPtyProcessClass = reinterpret_cast<jclass>(env->NewGlobalRef(localClass));
    env->DeleteLocalRef(localClass);

    gPtyProcessCtor = env->GetMethodID(gPtyProcessClass, "<init>", "(II)V");
    if (!gPtyProcessCtor) return JNI_ERR;

    return JNI_VERSION_1_6;
}