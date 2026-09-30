# Verified Career

**Verified Career** is a full-stack managed employment marketplace for two career tracks:

- **Tech and Corporate**
- **Trade / skilled workforce**

It goes beyond a conventional job board by combining verification, skill-aware matching, AI-assisted evaluation, job-specific assessments, employer-controlled ranking, hiring, placements, replacement support, training referrals, appointments, notifications, and a Bangla-first Trade experience.

---

## Table of Contents

1. [Key Features](#key-features)
2. [Technology Stack](#technology-stack)
3. [System Requirements](#system-requirements)
4. [Repository Structure](#repository-structure)
5. [Fresh Installation on Linux](#fresh-installation-on-linux)
6. [Fresh Installation on Windows](#fresh-installation-on-windows)
7. [Backend Configuration](#backend-configuration)
8. [Frontend Configuration](#frontend-configuration)
9. [Enable Gemini AI](#enable-gemini-ai)
10. [Optional Showcase Data](#optional-showcase-data)
11. [Running the Project](#running-the-project)
12. [Testing and Build Checks](#testing-and-build-checks)
13. [Database Notes](#database-notes)
14. [Updating an Existing Installation](#updating-an-existing-installation)
15. [Common Problems and Fixes](#common-problems-and-fixes)
16. [Security Notes](#security-notes)
17. [Production Notes](#production-notes)

---

# Key Features

## Accounts and authentication

- Candidate and Employer self-registration
- Login using either email or a Bangladeshi mobile number
- Backend phone-number normalization
- JWT authentication
- Role-based access control
- Roles: Candidate, Employer, Evaluator, Admin

## Candidate tracks

### Tech and Corporate

For students, developers, engineers, and corporate professionals.

### Trade

For skilled workers, with a Bangla-first system interface, Trade-specific skills, Bangla guidance, and browser-based voice assistance where supported.

## Verification

- Candidate verification
- Employer verification
- Configurable verification requirements
- Admin/Evaluator review workflows
- Optional candidate supporting documents
- Protected verification-file access

## Candidate profile and CV

- Profile photo
- Phone and contact email
- Location and experience
- Availability control
- Track-specific skills
- Structured CV Builder
- PDF CV upload and text extraction
- Portfolio information
- Job experience/history

## Jobs and applications

- Verified Employer job publication
- Tech/Trade validation
- Required-skill validation
- Employment type and location
- Job discovery filters
- Portal closing date and maximum portal lifetime
- Paid reopening of closed normal job portals
- Candidate verification, availability, and required-skill checks before application

## Candidate evaluation

Applicants can be evaluated using:

- CV
- Portfolio
- Experience
- Job-specific Assessment

Employers independently choose the weight of each component. The weights do **not** need to sum to `1`.

Availability affects applicant ordering but does not change the candidate's evaluation score.

## AI-assisted assessment

- Job-specific assessment after application
- Gemini integration on the backend
- Five-minute backend-authoritative deadline
- Refresh does not reset the timer
- Typed answers
- Press-and-hold speech input where the browser supports it
- Automatic final assessment evaluation

## Hiring and placements

- Application status workflow
- Shortlisting
- Final hiring
- Placement fee agreement
- One-time normal placement service fee based on the agreed first-month salary
- Hiring automatically makes the candidate unavailable
- Candidate can manually become available again
- Multiple active placements supported
- Candidate job history
- Candidate can mark a job as left

## Replacement management

- Admin-controlled replacement guarantee period
- Job-level guarantee starting from the first successful placement
- Guarantee duration snapshot per original job
- No additional placement fee for valid guaranteed replacements
- One shared free replacement vacancy for multiple outstanding replacements
- Replacement cancellation
- Trade FIFO skill matching alongside the shared replacement vacancy

## Training and appointments

- Admin-managed training programs
- Multi-skill training programs
- Admin/Evaluator referrals
- Track-aware candidate training visibility
- Evaluator appointment slots
- Consultation/interview bookings

## Other features

- Notifications and unread badge
- Demo payment flow
- Admin operational views
- Skill and company-type management
- Light/dark theme
- Collapsible sidebar
- Responsive frontend

---

# Technology Stack

## Backend

- **Java 21**
- **Spring Boot 4.1.1**
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Spring Validation
- MySQL
- JWT (`jjwt 0.13.0`)
- Apache PDFBox `3.0.8`
- Google GenAI Java SDK `1.73.0`
- Lombok
- Maven Wrapper

> A global Maven installation is not required because the repository includes `mvnw` and `mvnw.cmd`.

## Frontend

- **React 19**
- **TypeScript 7**
- **Vite 8**
- React Router
- Axios
- Tailwind CSS 4
- Oxlint

## Database

- **MySQL** for the application
- **H2** only for automated backend tests

---

# System Requirements

Install these before running the project:

| Requirement | Version |
|---|---|
| Git | Recent version |
| Java | **JDK 21** |
| Node.js | **22.12.0 or newer** |
| npm | Included with Node.js |
| MySQL | MySQL 8.x recommended |
| Browser | Current Chrome/Chromium recommended |

Default local ports:

| Service | Port |
|---|---:|
| Frontend | `5173` |
| Backend | `8080` |
| MySQL | `3306` |

Default development database:

```text
verified_career_marketplace
```

---

# Repository Structure

```text
First-Project/
├── backend/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .env.example
│   ├── setup-db.sh
│   ├── setup-db.ps1
│   ├── run-backend.sh
│   └── run-backend.ps1
├── frontend/
│   ├── src/
│   ├── package.json
│   ├── package-lock.json
│   └── .env.example
└── README.md
```

---

# Fresh Installation on Linux

These commands target Ubuntu/Debian-style systems. Package-install commands may differ on other Linux distributions.

## 1. Install Git and MySQL

```bash
sudo apt update
sudo apt install -y git mysql-server mysql-client curl
sudo systemctl enable --now mysql
```

Verify:

```bash
git --version
mysql --version
sudo systemctl status mysql
```

Press `q` to exit the service-status screen.

## 2. Install JDK 21

Install a standard **JDK 21** distribution.

If your configured repositories provide it:

```bash
sudo apt install -y openjdk-21-jdk
```

Verify:

```bash
java -version
javac -version
```

Both must report Java 21.

If your distribution does not provide `openjdk-21-jdk`, install any JDK 21 distribution such as Eclipse Temurin, Oracle JDK, or another OpenJDK 21 build, and ensure `java`/`javac` are in `PATH`.

## 3. Install Node.js 22+

One common option is NodeSource:

```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
```

Verify:

```bash
node --version
npm --version
```

Node must be at least `22.12.0`.

## 4. Clone the repository

```bash
git clone https://github.com/Nisa371/First-Project.git
cd First-Project
```

## 5. Configure the backend

```bash
cd backend
cp .env.example .env
nano .env
```

A suitable local starting configuration is:

```dotenv
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:5173

DB_HOST=localhost
DB_PORT=3306
DB_NAME=verified_career_marketplace
DB_USERNAME=marketplace
DB_PASSWORD=CHANGE_THIS_PASSWORD

JWT_TTL_SECONDS=3600

CV_STORAGE_DIRECTORY=uploads/cv

DEMO_ENABLED=false

AI_PROVIDER=UNCONFIGURED
GEMINI_API_KEY=
GEMINI_MODEL=gemini-3.8-flash
GEMINI_THINKING_LEVEL=low
AI_TIMEOUT=30s
```

Change `DB_PASSWORD`.

Save Nano with `Ctrl+O`, Enter, then `Ctrl+X`.

## 6. Create/configure MySQL

From `backend/`:

```bash
./setup-db.sh
```

The script creates the database and dedicated application user, grants access, and preserves existing data when re-run.

It normally uses local socket authentication through `sudo mysql`.

If your MySQL root/admin account uses password authentication:

```bash
./setup-db.sh --password-admin
```

If executable permissions are missing:

```bash
chmod +x setup-db.sh run-backend.sh mvnw
```

## 7. Start the backend

```bash
./run-backend.sh
```

Keep this terminal open.

Backend:

```text
http://localhost:8080
```

Health check:

```bash
curl http://localhost:8080/api/health
```

## 8. Install/configure the frontend

Open another terminal:

```bash
cd First-Project/frontend
cp .env.example .env
npm ci
```

`frontend/.env` should normally contain:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

## 9. Start the frontend

```bash
npm run dev
```

Open:

```text
http://localhost:5173
```

The local site is now running.

---

# Fresh Installation on Windows

Use PowerShell.

## 1. Install prerequisites

Install:

1. **Git for Windows**
2. **JDK 21**
3. **Node.js 22.12.0 or newer**
4. **MySQL Community Server 8.x**, including the command-line client

During MySQL installation, remember the administrative/root password.

Open a fresh PowerShell window and verify:

```powershell
git --version
java -version
javac -version
node --version
npm --version
mysql --version
```

If `mysql` is not in `PATH`, the included setup script also searches common MySQL installation directories.

## 2. Clone the repository

```powershell
git clone https://github.com/Nisa371/First-Project.git
cd First-Project
```

## 3. Configure the backend

```powershell
cd backend
Copy-Item .env.example .env
notepad .env
```

Use a configuration similar to:

```dotenv
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:5173

DB_HOST=localhost
DB_PORT=3306
DB_NAME=verified_career_marketplace
DB_USERNAME=marketplace
DB_PASSWORD=CHANGE_THIS_PASSWORD

JWT_TTL_SECONDS=3600

CV_STORAGE_DIRECTORY=uploads/cv

DEMO_ENABLED=false

AI_PROVIDER=UNCONFIGURED
GEMINI_API_KEY=
GEMINI_MODEL=gemini-3.8-flash
GEMINI_THINKING_LEVEL=low
AI_TIMEOUT=30s
```

Change `DB_PASSWORD`.

## 4. Make sure MySQL is running

```powershell
Get-Service *mysql*
```

If the service is called `MySQL80`, for example:

```powershell
Start-Service MySQL80
```

The actual service name may differ.

## 5. Prepare the database

From `backend/`:

```powershell
.\setup-db.ps1
```

The script asks for the MySQL administrative username. Press Enter to use `root`, then provide the administrator password when requested.

It creates:

- `verified_career_marketplace`
- the dedicated `marketplace` user
- required database grants

It also verifies the application-user connection.

### If PowerShell blocks local scripts

For the current PowerShell session only:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

Then retry:

```powershell
.\setup-db.ps1
```

### If `mysql.exe` cannot be found

Set its location manually, for example:

```powershell
$env:MYSQL_EXE="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
.\setup-db.ps1
```

## 6. Start the backend

```powershell
.\run-backend.ps1
```

Keep the terminal open.

Health endpoint:

```text
http://localhost:8080/api/health
```

## 7. Install/configure the frontend

Open a second PowerShell window:

```powershell
cd path\to\First-Project\frontend
Copy-Item .env.example .env
npm ci
```

`frontend/.env`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

## 8. Start the frontend

```powershell
npm run dev
```

Open:

```text
http://localhost:5173
```

---

# Backend Configuration

The helper scripts load:

```text
backend/.env
```

Do not commit the real `.env`.

Main settings:

```dotenv
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080

CORS_ALLOWED_ORIGINS=http://localhost:5173

DB_HOST=localhost
DB_PORT=3306
DB_NAME=verified_career_marketplace
DB_USERNAME=marketplace
DB_PASSWORD=your_private_password

JWT_TTL_SECONDS=3600

CV_STORAGE_DIRECTORY=uploads/cv

AI_PROVIDER=UNCONFIGURED
GEMINI_API_KEY=
GEMINI_MODEL=gemini-3.8-flash
GEMINI_THINKING_LEVEL=low
AI_TIMEOUT=30s
```

Default upload-storage locations also include:

```text
uploads/cv
uploads/photos
uploads/verification
```

The configured multipart upload limit is approximately **5 MB per file**.

## Important `.env` behavior

Use the included launch scripts:

Linux:

```bash
./run-backend.sh
```

Windows:

```powershell
.\run-backend.ps1
```

They load `backend/.env` before starting Spring Boot.

If you run Maven directly, export/set the environment variables yourself.

---

# Frontend Configuration

Create:

```text
frontend/.env
```

from `frontend/.env.example`.

Default:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

Important:

- include `/api`
- do not add a trailing slash
- never put database passwords, JWT secrets, or Gemini keys in `VITE_*` variables
- restart Vite after changing `.env`

---

# Enable Gemini AI

The application can start with AI disabled:

```dotenv
AI_PROVIDER=UNCONFIGURED
```

This is useful when testing non-AI functionality.

To enable Gemini-based evaluation, assessment, and Trade assistant functionality, edit `backend/.env`:

```dotenv
AI_PROVIDER=GEMINI
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
GEMINI_MODEL=gemini-3.8-flash
GEMINI_THINKING_LEVEL=low
AI_TIMEOUT=30s
```

Restart the backend afterward.

The API key belongs **only in the backend**.

If the configured model is unavailable to your Gemini account, set `GEMINI_MODEL` to a compatible model available to that API key.

---

# Optional Showcase Data

The backend contains an optional development showcase dataset for demonstrations.

In `backend/.env`:

```dotenv
DEMO_ENABLED=true
DEMO_PASSWORD=Choose-A-Private-Demo-Password
```

`DEMO_PASSWORD` must be between **10 and 72 bytes**.

On first startup against a fresh development database, the showcase initializer creates fictional users and marketplace records.

Example accounts include:

```text
admin@showcase.example.test
evaluator@showcase.example.test
employer@showcase.example.test
studio@showcase.example.test
trade1@showcase.example.test
trade2@showcase.example.test
trade3@showcase.example.test
trade4@showcase.example.test
trade5@showcase.example.test
trade6@showcase.example.test
tech1@showcase.example.test
tech2@showcase.example.test
```

All use the configured `DEMO_PASSWORD`.

The fixture also creates fictional candidates, employers, jobs, skills, placements, replacement/queue data, notifications, training/referral data, and an evaluator appointment slot.

**Use showcase mode on a fresh development database.**

After the data is created, setting `DEMO_ENABLED=false` stops further showcase initialization but does not remove existing records.

Never use demo credentials in production.

---

# Running the Project

Normal local architecture:

```text
MySQL :3306
    ↓
Spring Boot :8080
    ↓
React / Vite :5173
```

## Linux

Terminal 1:

```bash
cd First-Project/backend
./run-backend.sh
```

Terminal 2:

```bash
cd First-Project/frontend
npm run dev
```

## Windows

PowerShell 1:

```powershell
cd path\to\First-Project\backend
.\run-backend.ps1
```

PowerShell 2:

```powershell
cd path\to\First-Project\frontend
npm run dev
```

Then visit:

```text
http://localhost:5173
```

---

# Testing and Build Checks

## Backend tests

Linux:

```bash
cd backend
./mvnw test
```

Windows:

```powershell
cd backend
.\mvnw.cmd test
```

The backend automated-test profile uses H2 and does not require the local development MySQL database.

## Backend package

Linux:

```bash
./mvnw clean package
```

Windows:

```powershell
.\mvnw.cmd clean package
```

Output is created under:

```text
backend/target/
```

## Frontend lint

```bash
cd frontend
npm run lint
```

## Frontend production build

```bash
npm run build
```

This performs TypeScript compilation and a Vite production build.

Output:

```text
frontend/dist/
```

## Preview the built frontend

```bash
npm run preview
```

---

# Database Notes

## Fresh development database

The default Spring profile is `dev`.

The development profile uses MySQL and:

```text
spring.jpa.hibernate.ddl-auto=update
```

Therefore, on a **completely fresh** development installation:

1. create `backend/.env`
2. run `setup-db.sh` or `setup-db.ps1`
3. start the backend

Hibernate creates/updates the schema required by the current JPA entities.

## Historical SQL upgrades

The repository contains upgrade files such as:

```text
account-phone-upgrade.sql
candidate-contact-email-upgrade.sql
marketplace-agreement-upgrade.sql
marketplace-availability-upgrade.sql
marketplace-portals-upgrade.sql
marketplace-supporting-documents-upgrade.sql
training-placement-upgrade.sql
```

These are for upgrading databases created by **older versions** of the project.

**Do not execute all of them on a new empty database.**

---

# Updating an Existing Installation

Pull the newest code:

```bash
git pull
```

Update frontend dependencies:

```bash
cd frontend
npm ci
```

The Maven wrapper resolves backend dependencies automatically.

Some additive development-schema changes can be handled by Hibernate, but older project versions may require one or more explicit upgrade SQL scripts because of constraint/data changes.

Before changing an existing database:

1. back it up
2. determine which upgrade scripts have already been applied
3. apply only the scripts required for that old database
4. do not blindly run every SQL file

Example backup:

```bash
mysqldump -u marketplace -p verified_career_marketplace > verified_career_marketplace_backup.sql
```

Example restore:

```bash
mysql -u marketplace -p verified_career_marketplace < verified_career_marketplace_backup.sql
```

---

# Common Problems and Fixes

## Backend cannot connect to MySQL

Linux:

```bash
sudo systemctl status mysql
```

Windows:

```powershell
Get-Service *mysql*
```

Verify the following in `backend/.env`:

```dotenv
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

If you changed `DB_PASSWORD`, re-run the database setup helper.

Linux:

```bash
./setup-db.sh
```

Windows:

```powershell
.\setup-db.ps1
```

## `Access denied for user 'marketplace'`

The database password and `backend/.env` password do not match.

Re-run the database setup script.

## `mysql` command not found on Linux

```bash
sudo apt install mysql-client
```

## `mysql.exe` not found on Windows

Add MySQL's `bin` directory to `PATH`, or:

```powershell
$env:MYSQL_EXE="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
```

## Java version error

```bash
java -version
```

The backend requires **JDK 21**.

On Windows, if `JAVA_HOME` is set, it must point to the JDK directory rather than its `bin` directory.

## Node.js version error

```bash
node --version
```

Required:

```text
Node.js >= 22.12.0
```

## Port 5173 already in use

Stop the previous Vite process, or use another port:

```bash
npm run dev -- --port 5174
```

Then change backend CORS:

```dotenv
CORS_ALLOWED_ORIGINS=http://localhost:5174
```

and restart the backend.

## Port 8080 already in use

In `backend/.env`:

```dotenv
SERVER_PORT=8081
```

Then in `frontend/.env`:

```dotenv
VITE_API_BASE_URL=http://localhost:8081/api
```

Restart both services.

## Browser reports CORS errors

The frontend origin must match:

```dotenv
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Do not include `/api` in the CORS origin.

## Frontend opens but API requests fail

Check:

```text
http://localhost:8080/api/health
```

Then verify:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

Restart Vite after changing frontend environment variables.

## Gemini features fail

Check:

```dotenv
AI_PROVIDER=GEMINI
GEMINI_API_KEY=...
GEMINI_MODEL=...
```

Then restart the backend.

Provider failures may also be caused by quota limits, model availability, timeouts, or temporary upstream errors.

For non-AI development:

```dotenv
AI_PROVIDER=UNCONFIGURED
```

## Voice input does not work

Voice features depend on browser speech-recognition support.

For best results:

- use current Chrome/Chromium
- allow microphone permission
- use `localhost` or HTTPS
- confirm the operating system can access the microphone

Typing remains available where speech recognition is unsupported.

## PowerShell blocks `.ps1`

For the current terminal only:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

## PowerShell blocks `npm.ps1`

Use:

```powershell
npm.cmd ci
npm.cmd run dev
```

---

# Security Notes

- Never commit `backend/.env`.
- Never commit a real Gemini API key.
- Never expose backend secrets through `VITE_*`.
- Change sample/local database passwords.
- Use a strong `JWT_SECRET` outside development.
- Never use demo credentials in production.
- Treat CVs, photos, and verification documents as sensitive files.
- Restrict MySQL network access.
- Use HTTPS for non-local deployments.

A production JWT secret can be generated with:

```bash
openssl rand -base64 32
```

Then configure:

```dotenv
JWT_SECRET=your_generated_secret
```

---

# Production Notes

The installation steps above are primarily for local development/demo use.

The Spring `prod` profile uses:

```text
ddl-auto=validate
```

Therefore production expects the database schema to already be provisioned correctly; Hibernate will not create it automatically.

A production configuration should provide at least:

```dotenv
SPRING_PROFILES_ACTIVE=prod

DB_HOST=...
DB_PORT=3306
DB_NAME=...
DB_USERNAME=...
DB_PASSWORD=...

JWT_SECRET=...
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.example
```

If AI is enabled:

```dotenv
AI_PROVIDER=GEMINI
GEMINI_API_KEY=...
GEMINI_MODEL=...
```

A real deployment should also use:

- HTTPS
- private persistent upload storage
- restricted database networking
- backups
- service/process supervision
- a reverse proxy or managed hosting equivalent
- an explicit schema migration/deployment process

---

# Quick Start Summary

## Linux

```bash
git clone https://github.com/Nisa371/First-Project.git
cd First-Project/backend

cp .env.example .env
# Edit backend/.env

./setup-db.sh
./run-backend.sh
```

New terminal:

```bash
cd First-Project/frontend
cp .env.example .env
npm ci
npm run dev
```

Open:

```text
http://localhost:5173
```

## Windows

```powershell
git clone https://github.com/Nisa371/First-Project.git
cd First-Project\backend

Copy-Item .env.example .env
notepad .env

.\setup-db.ps1
.\run-backend.ps1
```

New PowerShell:

```powershell
cd path\to\First-Project\frontend
Copy-Item .env.example .env
npm ci
npm run dev
```

Open:

```text
http://localhost:5173
```

---

# Workflow Overview

```text
Registration
→ Profile
→ Verification
→ Job Discovery
→ Application
→ Candidate Evaluation
→ Assessment
→ Employer Ranking
→ Hiring
→ Placement
→ Replacement / Training / Career History
```

Verified Career is designed as one managed employment ecosystem while giving **Tech and Corporate** and **Trade** candidates experiences suited to their different needs.
