package io.github.karino2.bikeseibimemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.karino2.bikeseibimemo.ui.theme.BikeSeibiMemoTheme
import java.util.Date

class EditEntryActivity : ComponentActivity() {
    private val entryState = mutableStateOf(Entry(null, Category.GAS, Date(), null, null, null, ""))

    private var lastGasDist = -1

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {intn->
            val entry = intn.getSerializableExtra("ENTRY") as? Entry
            entry?.let { entryState.value = it }
            lastGasDist = intn.getIntExtra("LAST_GAS_DIST", -1)
        }

        setContent {
            BikeSeibiMemoTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            ),
                            title = {
                                Text("バイク整備メモ")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        Intent().apply {
                                            putExtra("ENTRY", entryState.value)
                                        }.also { setResult(RESULT_OK, it) }
                                        finish()
                                    }) {
                                        Icon(imageVector = Icons.Default.Done, contentDescription = "Save")
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                        }) },
                ) {
                    val entry = entryState.value
                    Summary(it, entry, if(lastGasDist == -1) null else lastGasDist, onEntryUpdate = { entryState.value = it })
                }
            }
        }
    }
}

@Composable
fun Summary(paddingVal: PaddingValues, entry : Entry, lastGasDist: Int?, onEntryUpdate: (Entry)->Unit) {
    if (entry.category == Category.GAS) {
        GasSummary(paddingVal, entry, lastGasDist, onEntryUpdate)
    } else {
        SeibiSummary(paddingVal, entry, onEntryUpdate)
    }
}

@Composable
fun GasSummary(paddingVal: PaddingValues, entry : Entry, lastGasDist: Int?, onEntryUpdate: (Entry)->Unit) {
    Column(modifier=Modifier.padding(paddingVal), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val openDatePicker = remember { mutableStateOf(false) }
        val openLiterPicker = remember { mutableStateOf(false) }
        val openPricePicker = remember { mutableStateOf(false) }
        val openMeterPicker = remember { mutableStateOf(false) }

        ItemRow("日付", entry.displayDate) { openDatePicker.value = true }
        ItemRow("給油量", entry.displayLiter) { openLiterPicker.value = true }
        ItemRow("金額", entry.displayPrice) { openPricePicker.value = true }
        Row {
            Text("単価", modifier=Modifier.width(100.dp))
            if (entry.liter != null && entry.price != null) {
                val tankaVal = entry.price.toDouble()/entry.liter
                val tanka = "%.2f円/l".format(tankaVal)
                Text(tanka)
            }
        }
        ItemRow("メーター", entry.displayRunningDistance) { openMeterPicker.value = true}
        if (lastGasDist != null) {
            Row {
                Text("燃費", modifier=Modifier.width(100.dp))
                if (entry.liter != null && entry.runningDistance != null) {
                    Text(EntryList.calcMileage(entry.runningDistance, entry.liter, lastGasDist))
                }
            }
        }
        Row {
            Text("メモ", modifier=Modifier.width(100.dp))
            TextField(value = entry.memo, modifier=Modifier.fillMaxWidth(), onValueChange = { onEntryUpdate(entry.copy(memo = it)) })
        }


        if (openDatePicker.value) {
            MyDatePicker(entry.date,
                { openDatePicker.value = false},
                {
                    openDatePicker.value = false
                    onEntryUpdate( entry.copy(date = it))
                } )
        }
        if (openLiterPicker.value) {
            DoublePicker(entry.liter, {openLiterPicker.value = false}, { openLiterPicker.value = false; onEntryUpdate( entry.copy(liter = it))})
        }
        if (openPricePicker.value) {
            IntPicker(entry.price, { openPricePicker.value = false }, {openPricePicker.value = false; onEntryUpdate( entry.copy(price=it)) })
        }
        if (openMeterPicker.value) {
            IntPicker(entry.runningDistance, { openMeterPicker.value = false }, {openMeterPicker.value = false; onEntryUpdate( entry.copy(runningDistance = it)) })
        }
    }
}

@Composable
fun SeibiSummary(paddingVal: PaddingValues, entry : Entry, onEntryUpdate: (Entry)->Unit) {
    Column(modifier=Modifier.padding(paddingVal)) {
        val openDatePicker = remember { mutableStateOf(false) }
        val openCategoryPicker = remember { mutableStateOf(false) }
        val openPricePicker = remember { mutableStateOf(false) }
        val openMeterPicker = remember { mutableStateOf(false) }

        ItemRow("日付", entry.displayDate) { openDatePicker.value = true }
        ItemRow("種類", entry.category.displayName) { openCategoryPicker.value = true }
        ItemRow("金額", entry.displayPrice) { openPricePicker.value = true }
        ItemRow("メーター", entry.displayRunningDistance) { openMeterPicker.value = true}

        Row {
            Text("メモ", modifier=Modifier.width(100.dp))
            TextField(value = entry.memo, modifier=Modifier.fillMaxWidth(), onValueChange = { onEntryUpdate(entry.copy(memo = it)) })
        }

        if (openDatePicker.value) {
            MyDatePicker(entry.date,
                { openDatePicker.value = false},
                {
                    openDatePicker.value = false
                    onEntryUpdate( entry.copy(date = it))
                } )
        }
        if (openCategoryPicker.value) {
            CategoryPicker({openCategoryPicker.value = false}, { openCategoryPicker.value = false; onEntryUpdate( entry.copy(category = it))})
        }
        if (openPricePicker.value) {
            IntPicker(entry.price, { openPricePicker.value = false }, {openPricePicker.value = false; onEntryUpdate( entry.copy(price=it)) })
        }
        if (openMeterPicker.value) {
            IntPicker(entry.runningDistance, { openMeterPicker.value = false }, {openMeterPicker.value = false; onEntryUpdate( entry.copy(runningDistance = it)) })
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePicker(initDate: Date, onDismiss: ()->Unit, onSelected: (Date)->Unit)
{
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initDate.time)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        dismissButton =  {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onSelected(Date(it))
                } ?: onDismiss()
            }) {
                Text("OK")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }

}

@Composable
fun NumButton(num: Int, onClick: (Int)->Unit, modifier: Modifier) {
    Button(modifier = modifier.fillMaxHeight().padding(2.dp), shape= RoundedCornerShape(5.dp),  onClick = { onClick(num) }) {
        Text(num.toString())
    }
}

@Composable
fun IntPicker(initVal: Int?, onDismiss: ()->Unit, onSelected: (Int)->Unit) {
    NumberPicker(false, initVal?.let { it.toString() } ?: "", onDismiss, onSelected = { onSelected(it.toInt())} )
}

@Composable
fun DoublePicker(initVal: Double?, onDismiss: ()->Unit, onSelected: (Double)->Unit) {
    NumberPicker(true, initVal?.let { "%.2f".format(it) } ?: "", onDismiss, onSelected = { onSelected(it.toDouble())} )
}

@Composable
fun CategoryButton(category: Category, onSelected: (Category)->Unit) {
    OutlinedButton(onClick = { onSelected(category) }) {
        Text(category.displayName, modifier=Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
fun CategoryPicker(onDismiss: ()->Unit, onSelected: (Category)->Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                CategoryButton(Category.OIL, onSelected)
                CategoryButton(Category.TIRE, onSelected)
                CategoryButton(Category.FILTER, onSelected)
                CategoryButton(Category.INSURANCE, onSelected)
                CategoryButton(Category.OTHER, onSelected)
            }
        }
    }
}


@Composable
fun NumberPicker(isFloat: Boolean, initVal: String, onDismiss: ()->Unit, onSelected: (String)->Unit) {
    val buttonShape = RoundedCornerShape(5.dp)
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8F)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            val num = remember { mutableStateOf(initVal) }
            Column {
                Text(num.value, modifier= Modifier
                    .align(Alignment.End)
                    .padding(10.dp), fontSize=30.sp)
                Row(modifier=Modifier.weight(1.0F)) {
                    NumButton(1, { num.value += "1" }, modifier=Modifier.weight(1.0F))
                    NumButton(2, { num.value += "2" }, modifier=Modifier.weight(1.0F))
                    NumButton(3, { num.value += "3" }, modifier=Modifier.weight(1.0F))
                }
                Row(modifier=Modifier.weight(1.0F)) {
                    NumButton(4, { num.value += "4" }, modifier=Modifier.weight(1.0F))
                    NumButton(5, { num.value += "5" }, modifier=Modifier.weight(1.0F))
                    NumButton(6, { num.value += "6" }, modifier=Modifier.weight(1.0F))
                }
                Row(modifier=Modifier.weight(1.0F)) {
                    NumButton(7, { num.value += "7" }, modifier=Modifier.weight(1.0F))
                    NumButton(8, { num.value += "8" }, modifier=Modifier.weight(1.0F))
                    NumButton(9, { num.value += "9" }, modifier=Modifier.weight(1.0F))
                }
                Row(modifier=Modifier.weight(1.0F)) {
                    NumButton(0, {
                        if(num.value != "0")
                            num.value += "0"
                     }, modifier=Modifier.weight(1.0F))

                    val bsweight = if(isFloat) 1.0F else 2.0F

                    if (isFloat) {
                        Button(modifier= Modifier
                            .weight(1.0F)
                            .padding(2.dp)
                            .fillMaxHeight(),
                            shape = buttonShape,
                            onClick= {
                            if(num.value != "" && !num.value.contains('.')) {
                                num.value = num.value + "."
                            }
                        }) {
                            Text(".", modifier=Modifier.align(Alignment.CenterVertically))
                        }

                    }

                    Button(modifier= Modifier
                        .weight(bsweight)
                        .padding(2.dp)
                        .fillMaxHeight(),
                        shape = buttonShape,
                        onClick= {
                        if(num.value != "") {
                            num.value = num.value.substring(0, num.value.length-1)
                        }
                    }) {
                        Text("BS", modifier=Modifier.align(Alignment.CenterVertically))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.weight(1.0F),
                    ) {
                        Text("Cancel", modifier= Modifier.padding(0.dp, 20.dp))
                    }
                    OutlinedButton(
                        onClick = { onSelected(num.value) },
                        modifier = Modifier.weight(1.0F),
                    ) {
                        Text("OK", modifier= Modifier.padding(0.dp, 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ItemRow(title: String, value: String, onClick: ()->Unit) {
    Row {
        Text(title, modifier=Modifier.width(100.dp).align(Alignment.CenterVertically))
        OutlinedButton(onClick = onClick, shape= RectangleShape) {
            Text(value)
        }
    }

}