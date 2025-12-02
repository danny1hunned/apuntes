@echo off
echo ============================================
echo  🧹 Cleaning Android Studio / Gradle caches
echo ============================================
cd /d "%~dp0"

REM 1. Delete Gradle build outputs
echo Deleting /app/build and /build folders...
rmdir /s /q "app\build"
rmdir /s /q "build"

REM 2. Delete Gradle caches
echo Deleting Gradle caches...
rmdir /s /q "%USERPROFILE%\.gradle\caches"

REM 3. Delete Android Studio caches
echo Deleting Android Studio caches...
rmdir /s /q "%USERPROFILE%\.android\build-cache"

REM 4. Run Gradle clean and resync
echo Running Gradle clean...
call gradlew clean

echo ============================================
echo  ✅ Cleanup done! Now open Android Studio and:
echo     1️⃣ File → Sync Project with Gradle Files
echo     2️⃣ Build → Rebuild Project
echo ============================================
pause
