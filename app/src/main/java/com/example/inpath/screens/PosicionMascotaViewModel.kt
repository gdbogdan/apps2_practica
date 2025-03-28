/*
* A través de este ViewModel, simulo la posicion inicial de la mascota, será en la C/ Jaume II, 71 (Edificio Polivalent 1)
* Y posteriormente se irá actualizando poco a poco como si la mascota se estuviese moviendo
*/
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class PosicionMascotaViewModel : ViewModel() {

    private val _posicion = MutableStateFlow(Posicion(41.60831345075668, 0.6234935707600733)) // Posición inicial en la EPS
    val posicion: StateFlow<Posicion> = _posicion

    init {
        actualizarPosicion()
    }

    private fun actualizarPosicion() {
        viewModelScope.launch {
            while (true) {
                val nuevaPosicion = simularMovimiento(posicion.value)
                _posicion.value = nuevaPosicion
                delay(10000) // Actualizar cada 10 segundos
            }
        }
    }

    private fun simularMovimiento(posicionActual: Posicion): Posicion {
        // Simulación de movimiento realista
        val velocidad = Random.nextDouble(0.0001, 0.00030) // Velocidad baja para simular movimiento realista
        val angulo = Random.nextDouble(0.0, 2 * Math.PI) // Ángulo aleatorio

        val nuevaLatitud = posicionActual.latitud + velocidad * cos(angulo)
        val nuevaLongitud = posicionActual.longitud + velocidad * sin(angulo)

        return Posicion(nuevaLatitud, nuevaLongitud)
    }
}

data class Posicion(val latitud: Double, val longitud: Double)