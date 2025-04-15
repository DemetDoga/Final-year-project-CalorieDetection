package com.example.caloriedetector
import android.Manifest

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.core.content.ContextCompat
import com.example.caloriedetector.ui.theme.CalorieDetectorTheme
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.*
import com.airbnb.lottie.compose.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

val NUM_CLASSES = 101

class MainActivity : ComponentActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { imageBitmap: Bitmap? ->
            imageBitmap?.let {
                processImage(it)
            }
        }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permission", "Kamera izni verildi.")
        } else {
            Log.e("Permission", "Kamera izni reddedildi!")
            Toast.makeText(this, "Kamera izni gerekli!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            CalorieDetectorTheme {
                var showHomeScreen by remember { mutableStateOf(true) }

                if (showHomeScreen) {
                    HomeScreen(onStartCamera = { showHomeScreen = false })
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        CameraCapture(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
    fun findHealthyAlternatives(
        targetCalories: Int,
        context: Context
    ): List<List<Pair<String, Int>>> {
        val alternatives = mutableListOf<List<Pair<String, Int>>>()


        val inputStream = context.assets.open("healthy_food_dataset_101.csv")
        val reader = inputStream.bufferedReader()
        val lines = reader.readLines()


        val foodList = mutableListOf<Pair<String, Int>>()
        for (line in lines.drop(1)) {
            val tokens = line.split(",")
            val foodName = tokens[0].trim()
            val calories = tokens[1].toIntOrNull() ?: 0
            foodList.add(Pair(foodName, calories))
        }


        foodList.shuffle()


        fun findCombinations(
            items: List<Pair<String, Int>>,
            currentCombination: List<Pair<String, Int>>,
            currentCalories: Int
        ) {
            if (currentCalories in (targetCalories - 50)..(targetCalories + 50)) {
                alternatives.add(currentCombination)
                return
            }
            if (currentCalories > targetCalories || items.isEmpty()) return


            findCombinations(
                items.drop(1),
                currentCombination + items.first(),
                currentCalories + items.first().second
            )

            findCombinations(items.drop(1), currentCombination, currentCalories)
        }


        findCombinations(foodList, emptyList(), 0)


        if (alternatives.isEmpty() && foodList.isNotEmpty()) {
            alternatives.add(listOf(foodList.random()))
        }


        return alternatives.take(5)
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HealthyAlternativesScreen(
        alternatives: List<List<Pair<String, Int>>>,
        onBack: () -> Unit
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Healthy Alternatives")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Special alternatives for you!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))


                if (alternatives.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(alternatives) { combination ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFD1C4E9), Color(0xFFEDE7F6))
                                        )
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 4.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    combination.forEach { (name, calories) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fastfood,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "$name (${calories} kcal)",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val composition by rememberLottieComposition(
                            LottieCompositionSpec.RawRes(R.raw.no_data)
                        )
                        val progress by animateLottieCompositionAsState(
                            composition = composition,
                            iterations = LottieConstants.IterateForever
                        )

                        LottieAnimation(
                            composition = composition,
                            progress = progress,
                            modifier = Modifier.size(200.dp)
                        )

                        Text(
                            text = "Uygun kombinasyon bulunamadı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Go Back",
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
    }


    @Composable
    fun ExerciseRecommendationsScreen(
        exerciseRecommendations: String?,
        onBack: () -> Unit
    ) {

        val recommendationsList = exerciseRecommendations?.split("\n") ?: emptyList()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "The exercises you need to do",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (recommendationsList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Egzersiz önerisi bulunamadı.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recommendationsList.take(5)) { recommendation ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = recommendation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp)
                        .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Go Back",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }




    @Composable
    fun HomeScreen(onStartCamera: () -> Unit) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.background_animation))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LottieAnimation(
                composition = composition,
                progress = progress,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Real-time food recognition and calorie calculation application",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Button(
                    onClick = onStartCamera, // Kamera başlatmayı tetikle
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .height(50.dp)
                        .fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Take a Photo and Get Started")
                }
            }
        }
    }




    @Composable

    fun CameraCapture(modifier: Modifier = Modifier) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder().build()
                        preview.setSurfaceProvider(surfaceProvider)

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.bindToLifecycle(
                            context as LifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    }, ContextCompat.getMainExecutor(context))
                }
            },
            modifier = modifier.fillMaxSize()
        )

        Button(onClick = {
            takePicture.launch()  // Fotoğraf çekme işlemi başlatılıyor
        }) {
            Text("Take A Photo")
        }
    }

    fun loadLabels(context: Context): List<String> {
        val labels = mutableListOf<String>()
        val inputStream = context.assets.open("labels.txt")
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { labels.add(it) }
        }
        return labels
    }


    fun processImage(imageBitmap: Bitmap) {
        val model = loadModelFile(this)
        val labels = loadLabels(this)
        val input = preprocessImage(imageBitmap)
        val output = Array(1) { FloatArray(NUM_CLASSES) }


        model.run(input, output)

        val predictedClass = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        if (predictedClass != -1 && predictedClass < labels.size) {
            val predictedLabel = labels[predictedClass]
            showResult(predictedLabel)
        } else {
            showResult(null)
        }
    }

    fun getNutritionFromCSV(predictedFood: String, context: Context): String? {
        val inputStream = context.assets.open("realistic_food_nutrition_dataset.csv")
        val reader = inputStream.bufferedReader()
        val lines = reader.readLines()


        for (line in lines.drop(1)) {
            val tokens = line.split(",")
            val foodName = tokens[1].trim()

            if (foodName.equals(predictedFood, ignoreCase = true)) {

                return """
                Food: $foodName
                - Calori: ${tokens[2]} kcal
                - Fat: ${tokens[3]} g
                - Saturated Fat: ${tokens[4]} g
                - Monounsaturated Fat: ${tokens[5]} g
                - Polyunsaturated Fat:${tokens[6]} g
                - Carbohydrate: ${tokens[7]} g
                - Sugar: ${tokens[8]} g
                - Protein: ${tokens[9]} g
                - Fiber: ${tokens[10]} g
                - Cholesterol: ${tokens[11]} mg
            """.trimIndent()
            }
        }
        return null
    }

    fun calculateExercise(calories: Int, context: Context): String {
        val inputStream = context.assets.open("exercise_dataset.csv")
        val reader = inputStream.bufferedReader()
        val lines = reader.readLines()

        val activities = mutableListOf<String>()
        val burnRates = mutableListOf<Double>()


        for (line in lines.drop(1)) {
            val tokens = line.split(",")
            if (tokens.size > 2) {
                val activityName = tokens[0].trim()
                val burnRate = tokens[2].toDoubleOrNull() ?: 0.0

                if (burnRate > 0) {
                    activities.add(activityName)
                    burnRates.add(burnRate)
                } else {
                    Log.e("CalculateExercise", "Hatalı yakma oranı: $activityName ($burnRate)")
                }
            } else {
                Log.e("CalculateExercise", "Eksik sütunlar: $line")
            }
        }

        val recommendations = StringBuilder("Exercise Recommendations:\n")
        val uniqueActivities = mutableSetOf<String>()

        val sortedActivities = activities.zip(burnRates)
            .sortedByDescending { it.second }
            .take(10)

        for ((activity, burnRate) in sortedActivities) {
            if (uniqueActivities.add(activity)) {
                val timeNeeded =
                    if (burnRate > 0) (calories / burnRate) * 60 else 0.0 //
                recommendations.append("$activity: %.2f dakika\n".format(timeNeeded))
                if (uniqueActivities.size >= 5) break
            }
        }

        return recommendations.toString()
    }


    fun extractCaloriesFromNutritionInfo(nutritionInfo: String?): Int {
        Log.d("Nutrition Info", "Nutrition info content: $nutritionInfo")

        // "Kalori" kelimesi geçen satırı bul ve yalnızca sayıyı al
        return nutritionInfo?.lines()?.find { it.contains("Calori", ignoreCase = true) }
            ?.split(":") // ":" ile ayır
            ?.getOrNull(1) // İkinci parçayı al
            ?.replace(Regex("[^0-9]"), "")
            ?.toIntOrNull() ?: 0
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ResultScreen(
        predictedLabel: String?,
        nutritionInfo: String?,
        exerciseRecommendations: String?,
        onBackToHome: () -> Unit,
        onShowAlternatives: () -> Unit,
        onShowExercises: () -> Unit
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Results") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Food Analysis Results",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )


                predictedLabel?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Food: $predictedLabel",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            nutritionInfo?.let { info ->
                                Text(
                                    text = info,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }


                Text(
                    text = "Nutrient Distribution",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                listOf("Protein" to 0.7f, "Fat" to 0.5f, "Carbohydrate" to 0.8f).forEach { (label, value) ->
                    Text(
                        text = "$label: ${(value * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LinearProgressIndicator(
                        progress = value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))


                Button(
                    onClick = onShowAlternatives,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "The Alternatives",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "The Alternatives")
                    }
                }


                Button(
                    onClick = onShowExercises,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Exercise Recommendations",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Exercise Recommendations")
                    }
                }


                Button(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Main Page",
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }


                Spacer(modifier = Modifier.height(16.dp))
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.food))
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever
                )

                LottieAnimation(
                    composition = composition,
                    progress = progress,
                    modifier = Modifier.size(200.dp)
                )
            }
        }
    }


    @Composable
    fun SectionWithIcon(title: String, content: String, icon: ImageVector) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    fun showResult(predictedLabel: String?) {
        if (predictedLabel != null) {
            val nutritionInfo = getNutritionFromCSV(predictedLabel, this)
            val calories = extractCaloriesFromNutritionInfo(nutritionInfo)
            val exerciseRecommendations = calculateExercise(calories, this) // Egzersiz önerileri
            val healthyAlternatives = findHealthyAlternatives(calories, this)

            setContent {
                CalorieDetectorTheme {
                    ResultScreen(
                        predictedLabel = predictedLabel,
                        nutritionInfo = nutritionInfo,
                        exerciseRecommendations = exerciseRecommendations,
                        onBackToHome = {
                            setContent { HomeScreen(onStartCamera = { showCamera() }) }
                        },
                        onShowAlternatives = {
                            setContent {
                                HealthyAlternativesScreen(
                                    alternatives = healthyAlternatives,
                                    onBack = { showResult(predictedLabel) }
                                )
                            }
                        },
                        onShowExercises = {
                            setContent {
                                ExerciseRecommendationsScreen(
                                    exerciseRecommendations = exerciseRecommendations,
                                    onBack = { showResult(predictedLabel) }
                                )
                            }
                        }
                    )
                }
            }
        } else {
            setContent {
                CalorieDetectorTheme {
                    ResultScreen(
                        predictedLabel = null,
                        nutritionInfo = null,
                        exerciseRecommendations = null,
                        onBackToHome = {
                            setContent { HomeScreen(onStartCamera = { showCamera() }) }
                        },
                        onShowAlternatives = {},
                        onShowExercises = {}
                    )
                }
            }
        }
    }



    fun showCamera() {
        // Kamera işlemini başlat
        takePicture.launch()
    }
}


