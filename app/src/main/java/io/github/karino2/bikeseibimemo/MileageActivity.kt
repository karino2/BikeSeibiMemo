package io.github.karino2.bikeseibimemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.karino2.bikeseibimemo.ui.theme.BikeSeibiMemoTheme

class MileageActivity : ComponentActivity() {

    private val entryListState by lazy { mutableStateOf( EntryList.load(this) ) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                Text("燃費")
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }) },
                ) {paddingVal->
                    LazyColumn(modifier= Modifier
                        .padding(paddingVal),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(entryListState.value.gasPair()) { (cur, prev) ->
                            Mileage(cur, prev)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Mileage(cur: Entry, prev: Entry) {
    val curDist = cur.runningDistance ?: return
    val prevDist = prev.runningDistance ?: return
    val liter = cur.liter ?: return

    val mileage = EntryList.calcMileage(curDist, liter, prevDist)

    Row(modifier=Modifier.fillMaxWidth().padding(5.dp)) {
        Text(cur.displayYearDate, fontWeight = FontWeight.Bold)
        Text(mileage, modifier=Modifier.weight(1.0F), textAlign = TextAlign.Center)
    }
}

