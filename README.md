NexTerm - Professional Terminal Emulator for Android

NexTerm Logo
Platform
Min SDK
Target SDK
Kotlin
Jetpack Compose
License

The Most Powerful Terminal Emulator for Android

Develop • Deploy • Debug - All from Your Phone

Features • Installation • Usage • Architecture • Contributing


📖 Overview
NexTerm is a professional-grade terminal emulator for Android, built entirely with modern technologies like Kotlin and Jetpack Compose. It provides a full Linux-like terminal experience on your Android device, supporting everything from basic file operations to advanced development workflows.

Unlike other terminal emulators, NexTerm offers:

🎨 Beautiful Material 3 UI with 5 customizable themes
📦 Built-in package manager (50+ packages)
🔒 Security & pentesting tools
🌐 Web servers & databases
💻 Multiple programming languages
🖥️ Multi-session support
⚡ ANSI color support (256 colors)
✨ Features
🚀 Core Terminal Features
Full Terminal Emulation

ANSI escape sequence support
256-color palette
Unicode character support
Cursor styles & blinking
Scrollback buffer (up to 50,000 lines)
Multi-Session Management

Create unlimited terminal sessions
Switch between sessions seamlessly
Session persistence
Individual working directories per session
Advanced Input Methods

Extra keys bar (ESC, TAB, CTRL, ALT, Arrows)
Hardware keyboard support
Gesture controls
Command history
Auto-completion ready
📦 Package Management
NexTerm includes a built-in package manager supporting 50+ packages:

Programming Languages
Bash

pkg install python      # Python 3.11
pkg install nodejs      # Node.js 18
pkg install php         # PHP 8.2
pkg install ruby        # Ruby 3.2
pkg install go          # Go 1.20
pkg install rust        # Rust 1.70
pkg install gcc         # GCC 12 (C/C++)
Web Servers & Databases
Bash

pkg install nginx       # Nginx 1.24
pkg install apache      # Apache 2.4
pkg install mysql       # MySQL 8.0
pkg install postgresql  # PostgreSQL 15
pkg install redis       # Redis 7.0
pkg install mongodb     # MongoDB 6.0
Security & Pentesting
Bash

pkg install nmap        # Network scanner
pkg install metasploit  # Penetration testing
pkg install hydra       # Password cracker
pkg install sqlmap      # SQL injection
pkg install aircrack-ng # WiFi security
pkg install wireshark   # Protocol analyzer
Development Tools
Bash

pkg install git         # Version control
pkg install vim         # Text editor
pkg install nano        # Simple editor
pkg install docker      # Containers
pkg install tmux        # Terminal multiplexer
pkg install make        # Build automation
View all packages →

🎨 Customization
Color Themes
Monokai - Classic dark theme
Dracula - Popular purple theme
Nord - Arctic blue theme
Solarized - Eye-friendly theme
Material - Modern Material Design
Font Options
Adjustable font size (8sp - 24sp)
Monospace font family
Line spacing control
Terminal Settings
Cursor blink animation
Keep screen on
Bell notifications
Vibrate on bell
Extra keys configuration
📁 File Management
Built-in file manager with:

Browse all directories
Create/Edit/Delete files & folders
Copy/Cut/Paste operations
Rename & move files
File properties & permissions
Hidden files toggle
Multi-select support
Quick access to common locations
🌐 What You Can Do
💻 Development
Full-stack web development (React, Vue, Angular)
Backend development (Node.js, Python, PHP)
Mobile app backends & REST APIs
Database design & management
Version control with Git
Code editing with Vim/Nano
🖥️ Server Management
Run HTTP servers (Nginx, Apache)
Database servers (MySQL, PostgreSQL, Redis)
SSH server for remote access
FTP server setup
WebSocket servers
API development & testing
📚 Learning & Education
Learn Linux commands
Shell scripting tutorials
Programming language practice
System administration
Network fundamentals
Security concepts
🔐 Security & Pentesting
Network scanning & mapping
Vulnerability assessment
Password security testing
SQL injection detection
WiFi security analysis
Packet sniffing & analysis
🤖 Automation
Bash/Shell scripting
Python automation scripts
Cron job scheduling
File operations automation
API automation
Task scheduling
🌐 Networking
Port scanning
Network monitoring
DNS lookups
HTTP/HTTPS testing
SSH connections
Network diagnostics
📱 Screenshots

Home Screen	Terminal	File Manager	Settings
Home	Terminal	Files	Settings
Multi-Session	Themes	Package Manager	Extra Keys
Sessions	Themes	Packages	Keys

🛠️ Installation
Prerequisites
Android 7.0 (API 24) or higher
100 MB free storage space
ARMv7, ARM64, x86, or x86_64 processor
Download
Option 1: GitHub Releases
Go to Releases
Download the latest nexterm-v1.0.0.apk
Install on your device
Option 2: Build from Source
Bash

# Clone the repository
git clone https://github.com/yourusername/nexterm.git
cd nexterm

# Build the APK
./gradlew assembleRelease

# Install on connected device
adb install app/build/outputs/apk/release/app-release.apk
🚀 Quick Start
First Launch
Open NexTerm - Launch the app
Create Session - Tap "New Terminal"
Start Coding - You're ready to go!
Basic Commands
Bash

# File operations
ls                    # List files
cd /sdcard           # Change directory
pwd                  # Print working directory
cat file.txt         # View file contents
mkdir newfolder      # Create directory
touch newfile.txt    # Create file

# System information
whoami               # Current user
uname -a             # System info
date                 # Current date/time
env                  # Environment variables

# Package management
pkg list             # List installed packages
pkg search python    # Search for packages
pkg install python   # Install package
pkg remove python    # Remove package

# Development
python script.py     # Run Python script
node app.js          # Run Node.js app
git clone <repo>     # Clone repository
vim file.txt         # Edit with Vim

# Networking
ping google.com      # Test connectivity
curl api.example.com # HTTP request
ssh user@host        # SSH connection
nmap 192.168.1.1     # Network scan

# Utilities
help                 # Show all commands
neofetch            # System info display
clear               # Clear screen
exit                # Exit terminal
Advanced Usage
Running a Web Server
Bash

# Install Nginx
pkg install nginx

# Start Nginx
nginx start

# Your server is now running on http://localhost:8080
Python Development
Bash

# Install Python
pkg install python

# Create a script
cat > hello.py << EOF
print("Hello from NexTerm!")
EOF

# Run it
python hello.py

# Install packages with pip
pip install requests flask numpy
Node.js Project
Bash

# Install Node.js
pkg install nodejs

# Initialize project
npm init -y

# Install packages
npm install express

# Create server
cat > server.js << EOF
const express = require('express');
const app = express();
app.get('/', (req, res) => res.send('Hello World!'));
app.listen(3000, () => console.log('Server running'));
EOF

# Run server
node server.js
Database Setup
Bash

# Install MySQL
pkg install mysql

# Start MySQL server
mysql start

# Connect to MySQL
mysql -u root

# Create database
CREATE DATABASE myapp;
USE myapp;
CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50));
🏗️ Architecture
Tech Stack
Language: Kotlin 2.0.21
UI Framework: Jetpack Compose
Architecture: MVVM + Clean Architecture
Dependency Injection: Koin
Database: Room
Preferences: DataStore
Async: Kotlin Coroutines & Flow
Navigation: Compose Navigation
Project Structure
text

app/
├── src/main/
│   ├── java/com/nexterm/app/
│   │   ├── data/                    # Data layer
│   │   │   ├── local/              # Local data sources
│   │   │   │   ├── database/       # Room database
│   │   │   │   ├── dao/            # Data Access Objects
│   │   │   │   ├── entity/         # Database entities
│   │   │   │   └── preferences/    # DataStore preferences
│   │   │   ├── model/              # Data models
│   │   │   └── repository/         # Repository implementations
│   │   │
│   │   ├── di/                     # Dependency Injection
│   │   │   ├── AppModule.kt
│   │   │   ├── RepositoryModule.kt
│   │   │   ├── ViewModelModule.kt
│   │   │   └── TerminalModule.kt
│   │   │
│   │   ├── terminal/               # Terminal engine
│   │   │   ├── TerminalEmulator.kt
│   │   │   ├── TerminalSessionManager.kt
│   │   │   ├── buffer/             # Terminal buffer
│   │   │   ├── ansi/               # ANSI parser
│   │   │   ├── shell/              # Shell executor
│   │   │   ├── pty/                # PTY implementation
│   │   │   ├── commands/           # Built-in commands
│   │   │   ├── service/            # Foreground service
│   │   │   └── receiver/           # Broadcast receivers
│   │   │
│   │   ├── package_manager/        # Package management
│   │   │   └── PackageManager.kt
│   │   │
│   │   └── presentation/           # UI layer
│   │       ├── theme/              # Theme & colors
│   │       ├── navigation/         # Navigation graph
│   │       ├── components/         # Reusable components
│   │       └── screens/            # Screen composables
│   │           ├── main/
│   │           ├── terminal/
│   │           ├── files/
│   │           └── settings/
│   │
│   └── res/                        # Resources
│       ├── values/
│       └── xml/
│
├── build.gradle.kts
└── proguard-rules.pro
Key Components
Terminal Emulator
ANSI Parser: Parses ANSI escape sequences
Terminal Buffer: Manages screen content
Shell Executor: Executes shell commands
Session Manager: Handles multiple sessions
Data Flow
text

User Input → ViewModel → Shell Executor → Process
                ↓
          Terminal Buffer ← ANSI Parser ← Process Output
                ↓
            UI Update
🔧 Building from Source
Requirements
Android Studio Hedgehog (2023.1.1) or later
JDK 17 or higher
Android SDK 35
Gradle 8.7+
Setup
Bash

# Clone repository
git clone https://github.com/yourusername/nexterm.git
cd nexterm

# Open in Android Studio
# Or build from command line:

# Debug build
./gradlew assembleDebug

# Release build (requires signing)
./gradlew assembleRelease

# Install on device
./gradlew installDebug
Configuration
Signing Configuration (for release builds)
Create keystore.properties in project root:

properties

storePassword=yourStorePassword
keyPassword=yourKeyPassword
keyAlias=yourKeyAlias
storeFile=path/to/keystore.jks
Update app/build.gradle.kts:

Kotlin

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"])
            storePassword = keystoreProperties["storePassword"]
            keyAlias = keystoreProperties["keyAlias"]
            keyPassword = keystoreProperties["keyPassword"]
        }
    }
}
📦 Available Packages
<details> <summary><b>Programming Languages (15)</b></summary>
Python 3.11 - pkg install python
Node.js 18 - pkg install nodejs
PHP 8.2 - pkg install php
Ruby 3.2 - pkg install ruby
Go 1.20 - pkg install go
Rust 1.70 - pkg install rust
GCC 12 - pkg install gcc
Clang 15 - pkg install clang
Java (OpenJDK) - pkg install openjdk
Perl - pkg install perl
Lua - pkg install lua
R - pkg install r-base
Erlang - pkg install erlang
Elixir - pkg install elixir
Julia - pkg install julia
</details><details> <summary><b>Web Servers (5)</b></summary>
Nginx 1.24 - pkg install nginx
Apache 2.4 - pkg install apache
Lighttpd - pkg install lighttpd
Caddy - pkg install caddy
Tomcat - pkg install tomcat
</details><details> <summary><b>Databases (8)</b></summary>
MySQL 8.0 - pkg install mysql
PostgreSQL 15 - pkg install postgresql
MongoDB 6.0 - pkg install mongodb
Redis 7.0 - pkg install redis
SQLite 3.41 - pkg install sqlite
MariaDB - pkg install mariadb
CouchDB - pkg install couchdb
InfluxDB - pkg install influxdb
</details><details> <summary><b>Security Tools (10)</b></summary>
Nmap 7.93 - pkg install nmap
Metasploit 6.3 - pkg install metasploit
Hydra 9.5 - pkg install hydra
SQLMap 1.7 - pkg install sqlmap
Aircrack-ng 1.7 - pkg install aircrack-ng
Wireshark 4.0 - pkg install wireshark
Netcat - pkg install netcat
TCPDump - pkg install tcpdump
Ncrack - pkg install ncrack
John the Ripper - pkg install john
</details><details> <summary><b>Development Tools (12)</b></summary>
Git 2.40 - pkg install git
Vim 9.0 - pkg install vim
Nano 7.0 - pkg install nano
Docker 24 - pkg install docker
Tmux 3.3 - pkg install tmux
Screen - pkg install screen
Make 4.4 - pkg install make
CMake - pkg install cmake
Maven - pkg install maven
Gradle - pkg install gradle
Ansible - pkg install ansible
Terraform - pkg install terraform
</details>
🎨 Customization Guide
Changing Themes
Open Settings
Go to Appearance → Color Scheme
Select your preferred theme:
Monokai
Dracula
Nord
Solarized
Material
Font Size Adjustment
Settings → Appearance → Font Size (8-24sp)

Extra Keys Configuration
Settings → Terminal → Configure Keys

Default layout:

text

ESC | TAB | CTRL | ALT | HOME | UP | END | PGUP
LEFT | DOWN | RIGHT | PGDN
Custom layout example:

text

CTRL | ALT | ESC | TAB | / | - | HOME | END
UP | DOWN | LEFT | RIGHT
Scrollback Buffer
Settings → Terminal → Scrollback Buffer (1000-50000 lines)

🐛 Troubleshooting
Common Issues
Terminal not responding
Bash

# Solution 1: Clear and restart
CTRL+C  # Stop current process
clear   # Clear screen

# Solution 2: Create new session
# Tap "+" button in top bar
Package installation fails
Bash

# Update package database
pkg update

# Try installing again
pkg install <package>
Permission denied errors
Bash

# Some commands require root
# NexTerm doesn't have root by default

# For file operations, use accessible directories:
cd /sdcard
cd ~/
App crashes on startup
Bash

# Clear app data
Settings → Apps → NexTerm → Clear Data

# Or reinstall the app
Getting Help
📖 Wiki
💬 Discussions
🐛 Issues
📧 Email: support@nexterm.app
🤝 Contributing
We love contributions! Here's how you can help:

Ways to Contribute
🐛 Report bugs
💡 Suggest features
📝 Improve documentation
🌍 Add translations
💻 Submit pull requests
Development Setup
Bash

# Fork the repository
# Clone your fork
git clone https://github.com/yourusername/nexterm.git

# Create a branch
git checkout -b feature/amazing-feature

# Make changes and commit
git commit -m "Add amazing feature"

# Push to your fork
git push origin feature/amazing-feature

# Open a Pull Request
Coding Standards
Follow Kotlin coding conventions
Use meaningful variable/function names
Add comments for complex logic
Write tests for new features
Update documentation
Commit Message Format
text

type(scope): subject

body (optional)

footer (optional)
Types: feat, fix, docs, style, refactor, test, chore

Example:

text

feat(terminal): add support for 24-bit colors

Implement true color support using RGB escape sequences.
Adds new color parsing logic in AnsiParser.

Closes #123
📄 License
text

MIT License

Copyright (c) 2024 NexTerm Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
🙏 Acknowledgments
Inspiration
Termux - The original Android terminal
iTerm2 - macOS terminal inspiration
Windows Terminal - Modern terminal features
Libraries & Tools
Jetpack Compose - Modern UI toolkit
Koin - Dependency injection
Room - Database
Material 3 - Design system
Contributors
Thanks to all contributors who help make NexTerm better!

<a href="https://github.com/yourusername/nexterm/graphs/contributors"> <img src="https://contrib.rocks/image?repo=yourusername/nexterm" /> </a>
📊 Stats
GitHub stars
GitHub forks
GitHub watchers
GitHub issues
GitHub pull requests
GitHub last commit
GitHub repo size
GitHub downloads

🗺️ Roadmap
Version 1.1 (Q2 2024)
 Plugin system
 SSH key management
 Syntax highlighting in editors
 Code auto-completion
 Floating window mode
Version 1.2 (Q3 2024)
 Widget support
 Shortcuts & macros
 Cloud sync (sessions, settings)
 Themes marketplace
 Advanced scripting
Version 2.0 (Q4 2024)
 X11 support (GUI apps)
 Container management UI
 Built-in code editor
 Integrated debugger
 Performance profiler
📞 Contact
Website: nexterm.app
Email: support@nexterm.app
Twitter: @NexTermApp
Discord: Join Server
Telegram: @NexTermOfficial
💖 Support the Project
If you find NexTerm useful, consider supporting us:

⭐ Star this repository
🐦 Share on social media
🐛 Report bugs
💡 Suggest features
📖 Improve documentation
☕ Buy us a coffee

Made with ❤️ by the NexTerm Team

Empowering developers on Android, one command at a time.

⬆ Back to Top

📚 Additional Resources
Tutorials
Getting Started with NexTerm
Package Management Guide
Advanced Terminal Usage
Shell Scripting Tutorial
Python Development on Android
Web Development with NexTerm
API Documentation
Terminal API Reference
Package Manager API
File System API
Examples
Sample Scripts
Configuration Files
Project Templates
🌍 Translations
Help us translate NexTerm to your language!

Currently supported:

🇺🇸 English
🇧🇩 Bengali (বাংলা)
