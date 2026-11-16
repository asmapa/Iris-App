# IRIS – Intelligent Real-Time Information System 👓

### AI-Powered Smart Glasses for Visually Impaired Individuals

IRIS (Intelligent Real-Time Information System) is an AI-powered assistive technology project designed to help visually impaired individuals understand their surroundings and access visual information independently.

The system uses computer vision, deep learning, Optical Character Recognition (OCR), and speech technologies to identify objects, recognize printed text, and communicate the results through audio feedback.

The primary goal of IRIS is to make everyday activities more accessible by reducing dependence on others for basic visual assistance.

---

## 🎯 Problem Statement

Visually impaired individuals often face difficulties performing everyday activities such as:

- Identifying objects and people in their surroundings.
- Recognizing printed text on books, notices, signboards, and documents.
- Understanding the visual environment around them.
- Accessing information that is primarily available in visual formats.

Traditional assistive devices may not provide real-time information about the user's surroundings.

IRIS aims to address these challenges through an intelligent, camera-based system that processes visual information and converts it into understandable audio output.

---

## 💡 Proposed Solution

IRIS integrates artificial intelligence and computer vision technologies into a smart-glass assistance concept.

A camera captures images or video frames from the user's surroundings. The system processes the visual input using object detection and OCR techniques.

The detected information is then converted into speech using a Text-to-Speech (TTS) engine, allowing users to receive information through audio rather than relying on visual displays.

The system also explores voice-based interaction to make the experience more accessible and user-friendly.

---

## ✨ Key Features

### 1. Real-Time Object Detection
- Detects objects from camera input.
- Uses YOLOv5 for object detection.
- Processes visual information using OpenCV.
- Announces detected objects through audio feedback.
- Helps users identify common objects in their surroundings.

### 2. Text Recognition (OCR)
- Extracts text from captured images.
- Uses Tesseract OCR to recognize printed text.
- Converts recognized text into speech.
- Helps users access information from printed materials, notices, and documents.

### 3. Text-to-Speech (TTS)
- Converts recognized text and detection results into spoken audio.
- Uses the pyttsx3 library.
- Provides audio-based feedback without requiring the user to read a screen.

### 4. Speech-Based Interaction
- Explores speech recognition and voice-based commands.
- Aims to provide a hands-free interaction experience.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Python | Core application development |
| OpenCV | Image processing and camera handling |
| YOLOv5 | Real-time object detection |
| PyTorch | Deep learning model execution |
| Tesseract OCR | Text extraction from images |
| Pytesseract | Python integration for Tesseract |
| pyttsx3 | Text-to-Speech conversion |
| Camera / ESP32-CAM | Visual input source |
| React.js | Web-based simulator interface |
| Tailwind CSS | Responsive UI styling |

*Note: The React simulator and hardware integration are separate parts of the project architecture; actual deployment depends on the configured components.*

---

## ⚙️ System Architecture

The system follows a visual-input-to-audio-output workflow.

1. **Image Acquisition:** The camera captures images or video frames from the environment.
2. **Image Processing:** OpenCV processes the captured visual input.
3. **Object Detection:** YOLOv5 identifies objects present in the image.
4. **Text Recognition:** Tesseract OCR extracts readable text when text-reading functionality is requested.
5. **Information Processing:** The extracted information is prepared for audio output.
6. **Audio Feedback:** The TTS engine converts the processed information into spoken responses.

### System Workflow

Camera Input → Image Processing → Object Detection / OCR → Information Processing → Text-to-Speech → Audio Output

---

## 🧠 Project Modules

### Object Detection Module
Responsible for identifying objects in camera images using the YOLOv5 deep learning model.

### Text Recognition Module
Extracts text from images using Tesseract OCR and Pytesseract.

### Speech Output Module
Converts the processed information into audible speech using pyttsx3.

### Camera Input Module
Handles image acquisition from the configured camera source.

### User Interface / Simulator
Provides a web-based interface for demonstrating the IRIS smart-glass concept.

---

## 📁 Project Structure

```text
IRIS/
│
├── backend/
│   ├── main.py
│   ├── object_detection.py
│   ├── text_reader.py
│   ├── speak.py
│   ├── camera.py
│   │
│   └── models/
│       └── yolov5s.pt
│
├── frontend/
│   ├── src/
│   ├── public/
│   └── package.json
│
├── README.md
└── requirements.txt
```

*The structure above is a suggested organization. Adjust filenames and folders to match the actual repository.*

---

## 🚀 Expected Benefits

- Improves accessibility for visually impaired individuals.
- Helps identify surrounding objects.
- Makes printed information accessible through audio.
- Reduces dependence on visual interfaces.
- Encourages independent interaction with everyday environments.
- Demonstrates the practical application of AI in assistive technology.

---

## 🔮 Future Enhancements

The following features are potential future improvements:

- **Scene Description:** Generate meaningful descriptions of the user's surroundings.
- **Obstacle Awareness:** Detect nearby obstacles and provide warnings.
- **Navigation Assistance:** Explore voice-guided navigation.
- **GPS Integration:** Support location-based assistance.
- **Emergency Alerts:** Explore emergency notifications to caregivers.
- **Multilingual Voice Support:** Provide audio feedback in multiple languages.
- **Mobile Application:** Develop a companion application for configuration and additional assistance.
- **Haptic Feedback:** Explore vibration-based alerts for important events.

---

## 🌍 Social Impact

IRIS is more than a computer vision project. It explores how artificial intelligence can be used to solve real-world accessibility challenges.

By transforming visual information into audio feedback, IRIS aims to support visually impaired individuals in accessing information and interacting more independently with their surroundings.

---

## 👩‍💻 Project Objective

To develop an intelligent and accessible assistive system that combines computer vision, deep learning, OCR, and speech technologies to make everyday visual information accessible to visually impaired individuals.

---

**Project Name:** IRIS – Intelligent Real-Time Information System

**Domain:** Artificial Intelligence | Computer Vision | Assistive Technology

**Primary Language:** Python

**Target Users:** Visually Impaired Individuals
