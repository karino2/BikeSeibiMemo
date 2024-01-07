package io.github.karino2.bikeseibimemo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.karino2.bikeseibimemo.ui.theme.BikeSeibiMemoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date

class ListActivity : ComponentActivity() {
    private val scope = MainScope()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private val entryListState by lazy { mutableStateOf( EntryList.load(this) ) }

    private val startForEntry = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK)
        {
            result.data?.let {
                val entry = it.getSerializableExtra("ENTRY")!! as Entry
                val elist = entryListState.value
                val newList = elist.update(entry)
                entryListState.value = newList
                scope.launch(Dispatchers.IO) {
                    EntryList.save(this@ListActivity, newList)
                }
            }
        }
    }

    private fun gotoEditActivity(target: Entry)
    {

        Intent(this, EditEntryActivity::class.java).also{
            it.putExtra("ENTRY", target)
            startForEntry.launch(it)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BikeSeibiMemoTheme {
                var isGasChecked by remember {mutableStateOf(true) }
                var isSeibiChecked by remember {mutableStateOf(true) }


                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Switch(checked = isGasChecked, onCheckedChange = { isGasChecked = it } )
                                    Text("給油", fontSize = 16.sp)
                                    Spacer(Modifier.size(10.dp))
                                    Switch(checked = isSeibiChecked, onCheckedChange = { isSeibiChecked = it } )
                                    Text("整備", fontSize = 16.sp)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                        }) },
                ) {paddingVal->
                    /*
                    LazyColumn(modifier= Modifier
                        .padding(paddingVal)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {

                     */
                    LazyColumn(modifier= Modifier
                        .padding(paddingVal),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(entryListState.value.fromLatest()) {entry->
                            if ((entry.category == Category.GAS && isGasChecked) ||
                                (entry.category != Category.GAS && isSeibiChecked)) {
                                Row(modifier = Modifier.clickable(onClick = { gotoEditActivity(entry) })) {
                                    Column(modifier = Modifier.weight(0.8F)) {
                                        Text(entry.displayDateOnly, fontWeight = FontWeight.Bold)
                                        Text(entry.category.displayName, color = Color.Blue)
                                    }
                                    Column(modifier = Modifier.weight(2.2F)) {
                                        val distance = entry.runningDistance?.let { "${it}km"} ?: ""
                                        Text(distance)
                                        Text(entry.memo, color = Color.DarkGray, fontSize = 14.sp)
                                    }
                                    Column(modifier=Modifier.weight(1.0F)) {
                                        entry.price?.let {
                                            Text("${it}円", fontWeight = FontWeight.Bold, color = Color(190, 212, 0), modifier = Modifier.align(Alignment.End))
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
}

