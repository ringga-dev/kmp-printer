package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalibrationScreen(
    viewModel: PrinterViewModel,
    onBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val is80mm = config.paperWidth >= 80

    var step by remember { mutableStateOf(1) }
    var leftDot by remember { mutableStateOf("0") }
    var rightDot by remember { mutableStateOf(if (is80mm) "576" else "384") }
    
    val printStatus by viewModel.printStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(printStatus) {
        if (printStatus is ngga.ring.printer.model.PrintStatus.Error) {
            snackbarHostState.showSnackbar((printStatus as ngga.ring.printer.model.PrintStatus.Error).message)
            viewModel.resetPrintStatus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Calibration Wizard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            // Stepper Visual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepCircle(1, step >= 1)
                Box(Modifier.width(40.dp).height(2.dp).background(if (step > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                StepCircle(2, step >= 2)
            }

            Spacer(Modifier.height(32.dp))

            AnimatedContent(targetState = step) { currentStep ->
                when (currentStep) {
                    1 -> Step1Content(
                        onPrint = { viewModel.printCalibrationPage() },
                        onNext = { step = 2 }
                    )
                    2 -> Step2Content(
                        leftDot = leftDot,
                        rightDot = rightDot,
                        onLeftChange = { leftDot = it },
                        onRightChange = { rightDot = it },
                        onFinish = {
                            viewModel.applyCalibration(
                                leftDot.toIntOrNull() ?: 0,
                                rightDot.toIntOrNull() ?: 576
                            )
                            onBack()
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState, 
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )

        val status = printStatus
        if (status !is ngga.ring.printer.model.PrintStatus.Idle && 
            status !is ngga.ring.printer.model.PrintStatus.Error && 
            status !is ngga.ring.printer.model.PrintStatus.Success) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            when (status) {
                                ngga.ring.printer.model.PrintStatus.Connecting -> "Connecting to Printer..."
                                ngga.ring.printer.model.PrintStatus.Processing -> "Generating Receipt..."
                                ngga.ring.printer.model.PrintStatus.Sending -> "Sending Data..."
                                else -> "Printing..."
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepCircle(num: Int, active: Boolean) {
    Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                num.toString(),
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun Step1Content(onPrint: () -> Unit, onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Print, contentDescription = null, Modifier.size(64.dp), Color.Gray)
        Spacer(Modifier.height(16.dp))
        Text("Step 1: Physical Test", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Print the calibration ruler to see where your paper physically starts and ends.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onPrint,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("Print Ruler Now")
        }
        TextButton(onClick = onNext) {
            Text("I already have the printout")
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
fun Step2Content(
    leftDot: String,
    rightDot: String,
    onLeftChange: (String) -> Unit,
    onRightChange: (String) -> Unit,
    onFinish: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Straighten, contentDescription = null, Modifier.size(64.dp), Color.Gray)
        Spacer(Modifier.height(16.dp))
        Text("Step 2: Analysis", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Look at the printed ruler. Which dot numbers are at the very edges of your paper?",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        
        OutlinedTextField(
            value = leftDot,
            onValueChange = onLeftChange,
            label = { Text("Left-most visible dot (e.g. 40)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.KeyboardArrowLeft, null) }
        )
        
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = rightDot,
            onValueChange = onRightChange,
            label = { Text("Right-most visible dot (e.g. 580)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.KeyboardArrowRight, null) }
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Apply Calibration")
        }
    }
}
