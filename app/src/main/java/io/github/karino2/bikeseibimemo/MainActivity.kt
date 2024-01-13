package io.github.karino2.bikeseibimemo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.karino2.bikeseibimemo.ui.theme.BikeSeibiMemoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date

class MainActivity : ComponentActivity() {
    private val scope = MainScope()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    val lastUri : Uri?
        get() = EntryList.lastUri(this)

    fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private val startForUrl = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK)
        {
            entryListState.value = loadEntryList()
        }
    }

    private val startForEntry = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK)
        {
            result.data?.let {
                val entry = it.getSerializableExtra("ENTRY")!! as Entry
                val elist = entryListState.value
                val newList = elist.add(entry)
                entryListState.value = newList
                scope.launch(Dispatchers.IO) {
                    EntryList.save(this@MainActivity, newList)
                }
            }
        }
    }

    private fun gotoSetup()
    {
        Intent(this, SetupActivity::class.java)
            .also { startForUrl.launch(it) }
    }

    private fun loadEntryList() : EntryList {
        return lastUri?.let { EntryList.load(this) } ?: EntryList(emptyList())
    }

    private val entryListState by lazy { mutableStateOf( loadEntryList() ) }

    private fun gotoGasEntry()
    {
        Intent(this, EditEntryActivity::class.java).also{
            it.putExtra("ENTRY", Entry(null, Category.GAS, Date(), null, null, null, ""))
            val lastdist = entryListState.value.lastCategoryBy(Category.GAS)?.runningDistance
            it.putExtra("LAST_GAS_DIST", lastdist)
            startForEntry.launch(it)
        }
    }

    private fun gotoSeibiEntry()
    {
        Intent(this, EditEntryActivity::class.java).also{
            it.putExtra("ENTRY", Entry(null, Category.OIL, Date(), null, null, null, ""))
            startForEntry.launch(it)
        }
    }

    private fun gotoListActivity() {
        Intent(this, ListActivity::class.java).also { startActivity(it) }
    }

    private fun gotoMileageActivity() {
        Intent(this, MileageActivity::class.java).also { startActivity(it) }
    }

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
                                Text("バイク整備メモ")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = { gotoSetup() }) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }

                            }
                        )
                    },
                    bottomBar = {
                        BottomAppBar(actions = {
                            Button(onClick = { gotoGasEntry() }) {
                                Text("給油")
                            }
                            Button(onClick = { gotoSeibiEntry() }) {
                                Text("整備")
                            }
                            Button(onClick = { gotoListActivity() }) {
                                Text("一覧")
                            }
                            Button(onClick = { gotoMileageActivity() }) {
                                Text("燃費")
                            }
                        })
                    }
                ) {innerPadding->
                    val entryList = entryListState.value
                    Column(modifier = Modifier.padding(innerPadding).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TitleContent("直近の燃費", entryList.lastMileage)
                        TitleContent("前回のオイル交換から", entryList.elapsedCategory(Category.OIL))
                        TitleContent("前回のフィルタ交換から", entryList.elapsedCategory(Category.FILTER))
                        TitleContent("前回のタイヤ交換から", entryList.elapsedCategory(Category.TIRE))
                        TitleContent("前回の自賠責保険から", entryList.elapsedCategory(Category.INSURANCE))
                    }
                }
            }
        }

        lastUri ?: return gotoSetup()
    }
}

@Composable
fun TitleContent(title: String, content: Pair<String, String>) {
    Column(modifier=Modifier.fillMaxWidth()) {
        Text(title, modifier=Modifier.align(Alignment.Start), color= Color.Blue )
        Row(modifier=Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(content.first)
            Text(content.second)
        }
    }
}
