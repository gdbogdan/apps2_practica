import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inpath.R

class InternetViewModel : ViewModel() {
    private val _isNetworkAvailable = MutableLiveData<Boolean>(false)
    val redDisponible: LiveData<Boolean> = _isNetworkAvailable

    private val _networkStatus = MutableLiveData<String>()
    val estadoRed: LiveData<String> = _networkStatus

    fun comprobarDisponibilidadRed(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val tieneInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        _isNetworkAvailable.postValue(tieneInternet)

        val mensaje = if (!tieneInternet) {
            context.getString(R.string.sinConexion)
        } else {
            when {
                isWifi -> context.getString(R.string.wi_fi_activado)
                isCellular -> context.getString(R.string.datos_activados)
                else -> context.getString(R.string.sinConexion) //Hay que ponerlo sino da error
            }
        }
        _networkStatus.postValue(mensaje)
    }
}