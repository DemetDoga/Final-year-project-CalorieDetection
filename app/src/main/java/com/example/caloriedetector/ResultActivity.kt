import android.content.res.AssetManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ChatBotModel(private val modelPath: String, private val assetManager: AssetManager) {

    private val interpreter: Interpreter

    init {
        val model = loadModelFile()
        interpreter = Interpreter(model)
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(inputText: String): String {
        val inputArray = tokenizeInput(inputText)

        val inputBuffer = ByteBuffer.allocateDirect(4 * inputArray.size).order(ByteOrder.nativeOrder())
        inputArray.forEach { inputBuffer.putFloat(it) }
        val outputBuffer = ByteBuffer.allocateDirect(4 * NUM_CLASSES).order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        val probabilities = FloatArray(NUM_CLASSES)
        outputBuffer.asFloatBuffer().get(probabilities)
        val predictedIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1

        return LABELS[predictedIndex]
    }

    private fun tokenizeInput(input: String): FloatArray {
        return FloatArray(100) { 0.0f } // Replace with actual tokenization logic
    }

    companion object {
        private const val NUM_CLASSES = 5
        private val LABELS = arrayOf("get_age", "get_weight", "get_height", "get_gender", "get_activity_level")
    }
}

fun calculateCalorieNeeds(data: Map<String, String>): Int {
    val age = data["get_age"]?.toIntOrNull() ?: 0
    val weight = data["get_weight"]?.toFloatOrNull() ?: 0f
    val height = data["get_height"]?.toFloatOrNull() ?: 0f
    val gender = data["get_gender"] ?: "Male"
    val activityLevel = data["get_activity_level"] ?: "Low"

    val bmr = if (gender == "Male") {
        88.36 + (13.4 * weight) + (4.8 * height) - (5.7 * age)
    } else {
        447.6 + (9.2 * weight) + (3.1 * height) - (4.3 * age)
    }

    val activityMultiplier = when (activityLevel) {
        "Low" -> 1.2
        "Medium" -> 1.55
        "High" -> 1.9
        else -> 1.2
    }

    return (bmr * activityMultiplier).toInt()
}

fun generateDietPlanMessage(calorieNeeds: Int, mealData: List<Map<String, Any>>): String {
    val plan = StringBuilder("Your 7-Day Diet Plan:\n")
    for (day in 1..7) {
        plan.append("Day $day:\n")
        var remainingCalories = calorieNeeds

        for (mealType in listOf("Kahvaltı", "Ara Öğün", "Öğle Yemeği", "Akşam Yemeği")) {
            val suitableMeals = mealData.filter {
                it["type"] == mealType && (it["calories"] as Int) <= remainingCalories
            }
            if (suitableMeals.isNotEmpty()) {
                val selectedMeal = suitableMeals.random()
                plan.append("- $mealType: ${selectedMeal["name"]} (${selectedMeal["calories"]} kcal)\n")
                remainingCalories -= selectedMeal["calories"] as Int
            }
        }
        plan.append("\n")
    }

    return plan.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotScreen(chatBotModel: ChatBotModel, mealData: List<Map<String, Any>>) {
    var message by remember { mutableStateOf("Welcome! Please enter your age.") }
    var userInput by remember { mutableStateOf("") }
    val userData = remember { mutableStateOf(mutableMapOf<String, String>()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ChatBot") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Your Answer") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val intent = chatBotModel.predict(userInput)
                userData.value[intent] = userInput
                userInput = ""

                message = when (intent) {
                    "get_age" -> "Enter your weight (kg):"
                    "get_weight" -> "Enter your height (cm):"
                    "get_height" -> "Specify your gender (Male/Female):"
                    "get_gender" -> "Choose your activity level (Low/Medium/High):"
                    "get_activity_level" -> {
                        val calorieNeeds = calculateCalorieNeeds(userData.value)
                        generateDietPlanMessage(calorieNeeds, mealData)
                    }
                    else -> "Thank you! Your data has been recorded."
                }
            }) {
                Text("Submit")
            }
        }
    }
}
