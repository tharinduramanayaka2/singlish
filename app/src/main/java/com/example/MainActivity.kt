package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainKeyboardScreen(
                    onOpenInputMethodSettings = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        startActivity(intent)
                    },
                    onOpenInputMethodPicker = {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainKeyboardScreen(
    onOpenInputMethodSettings: () -> Unit,
    onOpenInputMethodPicker: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isKeyboardEnabled by remember { mutableStateOf(false) }
    var isKeyboardSelected by remember { mutableStateOf(false) }
    var currentTheme by remember { mutableStateOf(ThemeManager.getCurrentTheme(context)) }
    var hapticEnabled by remember { mutableStateOf(ThemeManager.isHapticEnabled(context)) }
    var soundEnabled by remember { mutableStateOf(ThemeManager.isSoundEnabled(context)) }

    fun refreshKeyboardStatus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val enabledList = imm?.enabledInputMethodList ?: emptyList()
        val packageName = context.packageName

        isKeyboardEnabled = enabledList.any { it.packageName == packageName }

        val currentIme = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: ""
        isKeyboardSelected = currentIme.contains(packageName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKeyboardStatus()
                currentTheme = ThemeManager.getCurrentTheme(context)
                hapticEnabled = ThemeManager.isHapticEnabled(context)
                soundEnabled = ThemeManager.isSoundEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Setup & Test", "Themes", "Typing Guide", "Settings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ක",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Singlish Keyboard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Helakuru-style Phonetic Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTab) {
                0 -> SetupAndTestTab(
                    isKeyboardEnabled = isKeyboardEnabled,
                    isKeyboardSelected = isKeyboardSelected,
                    onOpenInputMethodSettings = onOpenInputMethodSettings,
                    onOpenInputMethodPicker = onOpenInputMethodPicker
                )
                1 -> ThemesTab(
                    currentTheme = currentTheme,
                    onThemeSelected = { newTheme ->
                        ThemeManager.setTheme(context, newTheme)
                        currentTheme = newTheme
                    }
                )
                2 -> TypingGuideTab()
                3 -> SettingsTab(
                    hapticEnabled = hapticEnabled,
                    onHapticToggled = {
                        ThemeManager.setHapticEnabled(context, it)
                        hapticEnabled = it
                    },
                    soundEnabled = soundEnabled,
                    onSoundToggled = {
                        ThemeManager.setSoundEnabled(context, it)
                        soundEnabled = it
                    }
                )
            }
        }
    }
}

@Composable
fun SetupAndTestTab(
    isKeyboardEnabled: Boolean,
    isKeyboardSelected: Boolean,
    onOpenInputMethodSettings: () -> Unit,
    onOpenInputMethodPicker: () -> Unit
) {
    var liveInputText by remember { mutableStateOf("mama lankaawata aadhareyi") }
    val transliterated = remember(liveInputText) {
        SinglishTransliterator.transliterate(liveInputText)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isKeyboardSelected) Color(0xFFECFDF5) else Color(0xFFEFF6FF)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isKeyboardSelected) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isKeyboardSelected) Color(0xFF059669) else Color(0xFF2563EB),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isKeyboardSelected) "Keyboard is Active & Ready!" else "Keyboard Setup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isKeyboardSelected) Color(0xFF065F46) else Color(0xFF1E3A8A)
                            )
                            Text(
                                text = if (isKeyboardSelected) "You can type in Sinhala anywhere on your device" else "Complete steps 1 and 2 to activate",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isKeyboardSelected) Color(0xFF047857) else Color(0xFF3B82F6)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1. Enable in Settings",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isKeyboardEnabled) "Enabled" else "Not enabled yet",
                                fontSize = 12.sp,
                                color = if (isKeyboardEnabled) Color(0xFF059669) else Color.Red
                            )
                        }
                        Button(
                            onClick = onOpenInputMethodSettings,
                            modifier = Modifier.testTag("btn_enable_ime"),
                            enabled = !isKeyboardEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text(if (isKeyboardEnabled) "Enabled" else "Enable")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "2. Select as Active Keyboard",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isKeyboardSelected) "Currently Default" else "Switch to Singlish Keyboard",
                                fontSize = 12.sp,
                                color = if (isKeyboardSelected) Color(0xFF059669) else Color.Gray
                            )
                        }
                        Button(
                            onClick = onOpenInputMethodPicker,
                            modifier = Modifier.testTag("btn_select_ime"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text(if (isKeyboardSelected) "Change" else "Select")
                        }
                    }
                }
            }
        }

        // Live Test Field to trigger soft keyboard
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Interactive Keyboard Test Field",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Tap below to pop up the custom keyboard and test live typing:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = liveInputText,
                        onValueChange = { liveInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_typing_input"),
                        label = { Text("Type here...") },
                        placeholder = { Text("e.g. mama lankaawata aadhareyi") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Live Transliteration Preview Simulator
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Transliteration Engine",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Phonetic",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Singlish Input:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (liveInputText.isEmpty()) "(Type something above)" else liveInputText,
                        color = Color(0xFFE2E8F0),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sinhala Output:",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (transliterated.isEmpty()) "..." else transliterated,
                        color = Color(0xFFFEF08A),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Predictions / Candidate Bar Preview:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val predictions = SinglishTransliterator.getPredictions(liveInputText, transliterated)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(predictions) { candidate ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable { liveInputText = candidate }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = candidate,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemesTab(
    currentTheme: ThemeManager.KeyboardTheme,
    onThemeSelected: (ThemeManager.KeyboardTheme) -> Unit
) {
    val themes = ThemeManager.KeyboardTheme.values()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Choose Keyboard Theme",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Themes apply seamlessly to the keyboard keys, candidate bar, and background.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(themes) { theme ->
            val isSelected = (theme == currentTheme)
            val colors = ThemeManager.getThemeColors(theme)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeSelected(theme) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF2563EB) else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = theme.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2563EB))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Active", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Theme Mini Mockup / Preview Strip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(colors.keyboardBackground))
                            .padding(10.dp)
                    ) {
                        Column {
                            // Mini Candidate Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(colors.candidateBarBackground))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(colors.candidatePrimaryChipBackground))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("මම", color = Color(colors.candidatePrimaryChipTextColor), fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(colors.candidateChipBackground))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("ඔයා", color = Color(colors.candidateChipTextColor), fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(colors.candidateChipBackground))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("කොහොමද", color = Color(colors.candidateChipTextColor), fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Mini Key Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(colors.keyBackground)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(key, color = Color(colors.keyTextColor), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypingGuideTab() {
    val rules = remember { SinglishTransliterator.getTypingGuide() }
    val categories = remember { rules.map { it.category }.distinct() }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredRules = remember(selectedCategory) {
        if (selectedCategory == "All") rules else rules.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Singlish Phonetic Cheat Sheet",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "How to write Sinhala words effortlessly using phonetic English keys:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val isSel = selectedCategory == "All"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) Color(0xFF2563EB) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedCategory = "All" }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("All", color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
                items(categories) { cat ->
                    val isSel = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) Color(0xFF2563EB) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(cat, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
            }
        }

        items(filteredRules) { rule ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rule.singlish,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF2563EB)
                            )
                            Text(
                                text = " ➔ ",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = rule.sinhala,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = rule.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = rule.category,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    hapticEnabled: Boolean,
    onHapticToggled: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundToggled: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Keyboard Feedback & Preferences",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Haptic feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Vibration / Haptic Feedback", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Vibrate when pressing keyboard keys", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = onHapticToggled,
                            modifier = Modifier.testTag("switch_haptic")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sound feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Key Sound Click", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Play soft audio click on keypress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = onSoundToggled,
                            modifier = Modifier.testTag("switch_sound")
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About Singlish Keyboard", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Built with Kotlin and InputMethodService. Featuring real-time phonetic transliteration, candidate prediction, and multi-theme customization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Version 1.0 (Production Architecture)", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
