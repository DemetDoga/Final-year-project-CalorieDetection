# 🍽️ Calorie Detector – Bachelor’s Thesis Project

This repository contains the source code of my **final year project** developed as part of my Bachelor's degree in Software Engineering. The project focuses on the integration of **machine learning** and **image processing** techniques to automatically detect food items and estimate their calorie values using a mobile application.

---

## 📱 About the App

The Calorie Detector is an Android application that allows users to:
- Take a photo or select an image from the gallery
- Automatically detect food items in the image using a trained AI model
- Display calorie estimations for the detected food
- Provide nutritional information using integrated CSV datasets

It is designed to help users track their dietary intake in a quick, visual, and user-friendly way.

---

## 🔍 Project Background

This application was developed during my final semester as a **Bachelor’s Thesis Project**. The idea was to create a tool that combines:
- **Computer Vision** (for food recognition)
- **Machine Learning** (for classification and calorie estimation)
- **Mobile App Development** (using Kotlin in Android Studio)

The project aims to demonstrate how AI can be applied to real-world health-related problems.

---

## 🛠️ Technologies & Tools

- Android Studio (Kotlin)
- TensorFlow Lite (for on-device ML)
- Custom-trained object detection model (YOLOv8)
- OpenCV (for image handling)
- CSV datasets (for food nutrition info)
- Git for version control

---

## 📊 Features

- 📷 Camera or Gallery image selection  
- 🍎 AI-based food detection from images  
- 🔢 Calorie estimation based on recognized food  
- 📁 Local nutrition datasets in CSV format  
- ⚡ Fast performance with on-device model (no internet needed)

---

## 🧠 Model & Datasets

The object detection model was trained externally using Python and YOLOv8 and then converted to **TensorFlow Lite (.tflite)** format to be used in the Android app.  
The app uses multiple CSV files to map detected food classes to nutritional values.

---



