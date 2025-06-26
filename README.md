# CameraApp 📸

A comprehensive Android application that demonstrates the differences between **CameraX** and **Camera Intent** APIs, featuring advanced OCR quality analysis and image comparison capabilities.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## 🚀 Overview

CameraApp is designed to help developers understand the practical differences between Android's modern CameraX API and the traditional Camera Intent approach. The app captures photos using both methods and provides detailed analysis including OCR quality ranking, image metadata comparison, and performance metrics.

### ✨ Key Features

- **📷 Dual Camera Implementation**: Side-by-side comparison of CameraX vs Camera Intent
- **🔍 OCR Quality Analysis**: ML Kit-powered text recognition with confidence scoring
- **📊 Image Quality Metrics**: Sharpness, contrast, brightness, and noise analysis
- **🏆 Intelligent Ranking**: Weighted scoring system for OCR suitability
- **📱 Material Design 3**: Modern UI with responsive layouts
- **🛡️ Comprehensive Error Handling**: Graceful degradation and user feedback
- **📋 Detailed Metadata**: EXIF data extraction and comparison
- **⚡ Performance Optimized**: Async processing with coroutines

## 🛠️ Technology Stack

### Core Technologies
- **Language**: Kotlin 100%
- **UI**: Traditional XML layouts with ViewBinding
- **Camera**: CameraX 1.3.4
- **OCR**: Google ML Kit Text Recognition 16.0.1
- **Image Loading**: Glide 4.16.0
- **Async**: Kotlin Coroutines

### Dependencies
```kotlin
// Camera & Image Processing
implementation("androidx.camera:camera-core:1.3.4")
implementation("androidx.camera:camera-camera2:1.3.4")
implementation("androidx.camera:camera-lifecycle:1.3.4")
implementation("androidx.camera:camera-view:1.3.4")

// ML Kit OCR
implementation("com.google.mlkit:text-recognition:16.0.1")
implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

// Image Processing
implementation("com.github.bumptech.glide:glide:4.16.0")
implementation("androidx.exifinterface:exifinterface:1.3.7")

// UI Components
implementation("com.google.android.material:material:1.11.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
```

## 📋 Requirements

### System Requirements
- **Android Studio**: Arctic Fox 2020.3.1 or later
- **Gradle**: 7.0+
- **JDK**: 8 or later
- **Android SDK**: API 21 (Android 5.0) to API 34 (Android 14)

### Device Requirements
- **Camera**: Hardware camera required
- **Storage**: 50MB free space
- **RAM**: 2GB minimum, 4GB recommended
- **Permissions**: Camera, Storage (for older Android versions)

### Supported Android Versions
- ✅ Android 5.0 (API 21) - Android 14 (API 34)
- ✅ Tested on devices with Android 8.0+ for optimal performance
- ✅ Package visibility queries for Android 11+ compatibility

## 🚀 Installation & Setup

### Option 1: Clone Repository
```bash
# Clone the repository
git clone https://github.com/angatiabenson/camera-app.git

# Navigate to project directory
cd camera-app

# Open in Android Studio
# File -> Open -> Select camera-app folder
```

### Option 2: Download ZIP
1. Download the latest release from [Releases](https://github.com/angatiabenson/camera-app)
2. Extract the ZIP file
3. Open in Android Studio

## 🎯 Usage Guide

### Basic Usage Flow

1. **Launch App**: Open CameraApp on your Android device
2. **Grant Permissions**: Allow camera access when prompted
3. **Capture with CameraX**: 
   - Tap "Take Photo with CameraX"
   - Use manual controls (flash, camera switch)
   - Capture your image
4. **Capture with Camera Intent**:
   - Tap "Take Photo with Camera Intent"
   - Use device's default camera app
   - Save and return to app
5. **Compare Results**:
   - Tap "Compare Images" once both photos are taken
   - View detailed OCR analysis and quality metrics
   - See winner determination and recommendations

### Advanced Features

#### CameraX Controls
- **Flash Modes**: Off, On, Auto
- **Camera Switch**: Front/Back camera toggle
- **Manual Focus**: Tap to focus (coming soon)
- **Exposure Control**: Manual exposure adjustment (coming soon)

#### OCR Analysis Features
- **Text Detection**: Automatic text recognition
- **Confidence Scoring**: ML Kit confidence levels
- **Quality Metrics**: Sharpness, contrast, brightness analysis
- **Readability Score**: Weighted OCR suitability ranking

## 🏗️ Project Structure

```
camera-app/
├── app/
│   ├── src/main/java/ke/co/banit/cameraapp/
│   │   ├── MainActivity.kt                # Main entry point
│   │   ├── CameraXActivity.kt             # CameraX implementation
│   │   ├── ComparisonActivity.kt          # Analysis & comparison
│   │   └── utils/
│   │       ├── ImageQualityAnalyzer.kt    # Image quality metrics
│   │       ├── OCRAnalyzer.kt             # OCR analysis utilities
│   ├── src/main/res/
│   │   ├── layout/                        # XML layouts
│   │   ├── values/                        # Resources (strings, colors)
│   │   ├── xml/                           # File provider paths
│   │   └── drawable/                      # Icons and drawables
│   └── src/main/AndroidManifest.xml       # App configuration
├── gradle/                                # Gradle wrapper
└── README.md                             # This file
```

## 🔧 Configuration

### Permissions Setup
The app requires these permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

### Package Visibility (Android 11+)
```xml
<queries>
    <intent>
        <action android:name="android.media.action.IMAGE_CAPTURE" />
    </intent>
</queries>
```

### FileProvider Configuration
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

## 🧪 Testing

### Manual Testing Checklist
- [ ] Camera permission handling
- [ ] CameraX photo capture with various settings
- [ ] Camera Intent photo capture
- [ ] OCR analysis with text-containing images
- [ ] Image quality comparison
- [ ] File storage and retrieval
- [ ] Error handling scenarios

### Test Scenarios
1. **Different Lighting Conditions**:
   - Bright daylight
   - Indoor lighting
   - Low light conditions
   - Mixed lighting

2. **Various Text Types**:
   - Printed documents
   - Handwritten text
   - Digital screens
   - Street signs

3. **Device Variations**:
   - Different Android versions
   - Various screen sizes
   - Different camera hardware

### Running Tests
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate test coverage report
./gradlew jacocoTestReport
```

## 📈 Performance Considerations

### Memory Optimization
- Bitmap processing with proper recycling
- Glide image loading with caching
- Coroutines for background processing
- Proper lifecycle management

### Storage Optimization
- Efficient file naming conventions
- Automatic cleanup of temporary files
- Compressed image storage options
- External storage usage patterns

### Processing Optimization
- Async OCR analysis
- Background thread image processing
- Lazy loading of comparison results
- Efficient quality metric calculations

## 🐛 Known Issues & Limitations

### Current Limitations
1. **OCR Language Support**: Currently optimized for Latin script
2. **Image Size**: Large images may cause memory issues on low-end devices
3. **Real-time Processing**: OCR analysis is post-capture only
4. **Advanced Camera Controls**: Limited manual camera controls in current version

## 📄 License

This project is licensed under the MIT License.

```
MIT License

Copyright (c) 2025 Benson Angatia

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
```

## 👨‍💻 Author

**Benson Angatia** ([@angatiabenson](https://github.com/angatiabenson))
- 📧 Email: angatiabenson1@gmail.com
- 💼 LinkedIn: [Benson Angatia](https://www.linkedin.com/in/angatia-benson/)
