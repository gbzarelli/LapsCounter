package br.com.helpdev.lapscounter.ui.viewmodel.objects

import androidx.lifecycle.MutableLiveData
import java.io.Serializable

class HeaderDistances : Serializable {
    val lapDistance = MutableLiveData<String>()
    val travelledDistance = MutableLiveData<String>()
    val metric = MutableLiveData<String>()
}
