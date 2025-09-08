# Canopus Project: Large-Scale Channel Parameter Estimation

This repository contains the data and source code for the project **Estimation of Large-Scale Wireless Channel Parameters (Path Loss and Shadow Fading)**.  
The dataset was collected using a custom-developed Android application, and the analysis was performed in Python to estimate the channel model parameters.  

---

## Repository Structure
.
├── WaveTrack/ # Source code of the Android app for data collection
├── Data/ # Collected dataset (CSV format)
├── Analysis/ # Python scripts for data analysis and parameter estimation
└── README.md # Project documentation


---

## Components

### 📱 Android App
- Android application for recording UE location, serving cell ID, RSRP, and other information.  
- Source code is available under `WaveTrack/`.  

### 📊 Data
- The collected dataset is provided in `CSV` format inside the `Data/` folder.  
- Each record includes the following fields:  
  - Cell ID  
  - Latitude, Longitude (UE location)  
  - RSRP (dBm)  
  - MCC, MNC, TAC  

### 🧮 Analysis
- Python scripts inside the `Analysis/` folder implement:  
  1. Data cleaning and preprocessing (removing irrelevant or corrupted records, filtering outliers)  
  2. Computation of UE–eNodeB distances  
  3. Linear regression in log-distance scale to estimate path loss exponent **β** and shadow fading standard deviation **σ**  
  4. Residual analysis and visualization (e.g., histograms, scatter plots)  

---

## Usage

### Run Analysis
1. Open the notebook:
   ```bash
   jupyter notebook analysis/Estimation.ipynb
2. Run the cells step by step to reproduce the results.

   
### Run Android App
* Import the project in Android Studio to build and run the app.
* Alternatively, download and install the APK from the Releases
