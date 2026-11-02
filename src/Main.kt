
//=============================================
//    PROGRAMACIÓN ORIENTADA A OBJETOS (POO)
//============================================

// Estados posibles de un pedido
enum class EstadoPedido(val texto: String) {
    EN_PREPARACION("En preparación"),
    LISTO("Listo"),
    EN_CAMINO("En camino")
}

//Clase base usuario (Herencia)
open class Usuario(val nombre: String, val contacto: String) {
    open fun mostrarDetalle() {
        println("Usuario: $nombre, contacto: $contacto")

    }
}

// Subclase Cliente (Validación de String, teléfono y correo)
class Cliente(nombre: String, contacto: String, val correo: String?, val zona: String) : Usuario(nombre, contacto) {
    fun validarTelefono(): String {
        // Acepta +56912345678 / 912345678 / (123) 456-7890
        val formato = Regex("^(\\+?\\d{9,13}|\\(\\d{3}\\) \\d{3}-\\d{4})$")
        return if (formato.matches(contacto)) contacto else "(000) 000-0000"
    }

    fun validarCorreo(): String {
        val formato = Regex("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")
        return if (correo != null && formato.matches(correo)) correo else "correo@invalido.com"

    }

    override fun mostrarDetalle() {
        println("Cliente: $nombre, teléfono: ${validarTelefono()}, correo: ${validarCorreo()}, zona: $zona")
    }

}
// Subclase Repartidor
class Repartidor(nombre: String, contacto: String, val zona: String) : Usuario(nombre, contacto) {
    fun entregarPedido(idPedido: Int){
        println("Repartidor $nombre está entregando el pedido #$idPedido en la zona $zona.")
    }

    override fun mostrarDetalle() {
        println("Repartidor: $nombre, contacto: $contacto, zona: $zona")
    }
}
// Clase producto
class Producto(val nombre: String, val precio: Double, val ingredientes: List<String>) {
    init {
        // Costo no válido: lo capturamos con try-catch donde se crea el producto
        require(precio > 0) { "El precio de '$nombre' debe ser mayor a 0 (se recibió $precio)." }
    }

    fun detallarProducto():String{
        return "Pizza_ $nombre | Precio: \$$precio | Ingredientes: ${ingredientes.joinToString(",")}"
    }
}
// Producto con su cantidad dentro de un pedido
class ItemPedido(val producto: Producto, val cantidad: Int) {
    init {
        require(cantidad > 0) { "La cantidad de '${producto.nombre}' debe ser mayor a 0 (se recibió $cantidad)." }
    }
}
// Clase Pedido
class  Pedido(
    val id: Int,
    val cliente: Cliente,
    val items: List<ItemPedido>,
    var estado: EstadoPedido,
    val notas: String? = null // Null safety para campos opcionales

) {
    init {
        require(items.isNotEmpty()) { "El pedido #$id debe tener al menos un producto." }
    }

    fun cantidadPizzas(): Int {
        var total = 0
        for (item in items) {
            total += item.cantidad
        }
        return total
    }

    fun calcularSubtotal (): Double {
        var subtotal = 0.0
        for (item in items) {
            subtotal += item.producto.precio * item.cantidad
        }
        return subtotal
    }

    fun calcularImpuestos (): Double {
        return calcularSubtotal() * 0.19 // 19% IVA
    }

    //Descuento del 10% si ordena más de 3 pizzas
    fun calcularDescuento (): Double {
        return if (cantidadPizzas() > 3) (calcularSubtotal() + calcularImpuestos()) * 0.10 else 0.0
    }

    fun calcularTotalFinal (): Double {
        return (calcularSubtotal() + calcularImpuestos()) - calcularDescuento()

    }

    fun cambiarEstado(nuevoEstado: EstadoPedido) {
        println("Pedido #$id: ${estado.texto} -> ${nuevoEstado.texto}")
        estado = nuevoEstado
    }
}

//============================================================
//            FUNCIONES REUTILIZABLES
//============================================================

// Formatea un monto para mostrarlo en pantalla
fun precio(valor: Double): String {
    return "\$${String.format("%.0f", valor)}"
}

// Evalúa con 'when' si el inventario alcanza para el pedido. Retorna los ingredientes faltantes (ingrediente -> unidades que faltan)
fun verificarDisponibilidad(pedido: Pedido, inventario: Map<String, Int>): Map<String, Int> {
    val necesarios = mutableMapOf<String, Int>()
    for (item in pedido.items) {
        for (ingrediente in item.producto.ingredientes) {
            necesarios[ingrediente] = (necesarios[ingrediente] ?: 0) + item.cantidad
        }
    }

    val faltantes = mutableMapOf<String, Int>()
    for ((ingrediente, necesario) in necesarios) {
        val stock = inventario[ingrediente] ?: 0
        when {
            stock == 0 -> {
                println("[CRÍTICO] Sin stock de $ingrediente (se necesitan $necesario).")
                faltantes[ingrediente] = necesario
            }
            stock < necesario -> {
                println("[ADVERTENCIA] Stock insuficiente de $ingrediente (hay $stock, se necesitan $necesario).")
                faltantes[ingrediente] = necesario - stock
            }
            else -> println("[OK] Ingrediente disponible: $ingrediente (hay $stock, se necesitan $necesario)")
        }
    }
    return faltantes
}

// Muestra los pasos para reabastecer los ingredientes insuficientes
fun mostrarPasosReabastecimiento(faltantes: Map<String, Int>) {
    println("Pasos para reabastecer:")
    var paso = 1
    for ((ingrediente, cantidad) in faltantes) {
        println("  $paso- Solicitar al proveedor al menos $cantidad unidades de $ingrediente.")
        paso++
    }
    println("  $paso- Registrar el ingreso en el inventario y volver a ingresar el pedido.")
}

// Dada una lista de ingredientes, calcula lo necesario para un lote de pizzas y actualiza el inventario
fun prepararLote(ingredientes: List<String>, cantidadPizzas: Int, inventario: MutableMap<String, Int>): Map<String, Int> {
    if (cantidadPizzas <= 0) {
        throw IllegalArgumentException("La cantidad de pizzas del lote debe ser mayor a 0.")
    }
    val necesarios = mutableMapOf<String, Int>()
    for (ingrediente in ingredientes.distinct()) {
        necesarios[ingrediente] = cantidadPizzas // 1 unidad de cada ingrediente por pizza
    }
    // Se valida todo antes de descontar para no dejar el inventario a medias
    for ((ingrediente, necesario) in necesarios) {
        val stock = inventario[ingrediente] ?: 0
        if (stock < necesario) {
            throw IllegalStateException("Inventario insuficiente de $ingrediente: hay $stock y se necesitan $necesario.")
        }
    }
    for ((ingrediente, necesario) in necesarios) {
        inventario[ingrediente] = inventario.getValue(ingrediente) - necesario
    }
    return necesarios
}

// Suma unidades al inventario, no acepta cantidades negativas
fun reabastecerIngrediente(inventario: MutableMap<String, Int>, ingrediente: String, cantidad: Int) {
    if (cantidad < 0) {
        throw IllegalArgumentException("La cantidad a reabastecer de $ingrediente no puede ser un número negativo.")
    }
    inventario[ingrediente] = (inventario[ingrediente] ?: 0) + cantidad
}

//============================================================
//            FUNCIÓN PRINCIPAL (MAIN)
//============================================================

fun main() {
    println("=========================================")
    println("    SISTEMA DE GESTIÓN PIZZERÍA LOCAL    ")
    println("=========================================")

    // 1- COLECCIONES: Map para inventario y List para repartidores
    val inventarioIngredientes = mutableMapOf(
        "Masa" to 20,
        "Salsa de Tomate" to 15,
        "Queso Mozzarella" to 10,
        "Pepperoni" to 5,
        "Queso Azul" to 6,
        "Tomate" to 8,
        "Piña" to 4,
        "Jamón" to 4
    )

    val repartidoresDisponibles = listOf(
        Repartidor( "Carlos Pérez", "+56911112222", "Centro"),
        Repartidor("Ana Gómez", "+56933334444", "La Florida"),
        Repartidor ("Luis Soto", "+56955556666","Vitacura")
    )

    // 2- MANEJO DE ERRORES (Try-Catch) Y ENTRADA DE DATOS (readline)
    println( "\n--- [PASO 4] Manejo de Errores en Inventario ---")
    try {
        println("Ingrese cantidad a reabastecer para Masa (ej: 5 o -2 para probar error): ")
        val entrada = readLine()
        val ajuste = entrada?.toInt() ?: throw IllegalArgumentException("Valor nulo o vacío")

        reabastecerIngrediente(inventarioIngredientes, "Masa", ajuste)
        println(" ¡Inventario actualizado! Nuevo stock de Masa: ${inventarioIngredientes["Masa"]}")

    } catch (e: NumberFormatException) {
        println("Error de formato: Debe ingresar un número entero válido. (${e.message})")
    } catch (e: IllegalArgumentException) {
        println("Error de validación: ${e.message}. Corrija los datos ingresados.")

    }

    // 3- REGISTRO DE CLIENTES Y VALIDACIÓN DE STRINGS (valores predeterminados si el formato es incorrecto)
    println("\n--- [PASO 1] Registro de Clientes ---")
    val cliente1 = Cliente("Guiselle Sepúlveda", "+569987654321", "guiselle@correo.com", "La Florida")
    val cliente2 = Cliente("Pedro Rojas", "12-34", null, "Providencia") // teléfono inválido y sin correo
    val cliente3 = Cliente("María Díaz", "(555) 123-4567", "maria.correo.com", "Vitacura") // correo inválido
    for (c in listOf(cliente1, cliente2, cliente3)) {
        c.mostrarDetalle()
    }

    // 4- INSTANCIACIÓN DE PRODUCTOS (con try-catch para costos no válidos)
    try {
        Producto("Pizza Promo", -500.0, listOf("Masa"))
    } catch (e: IllegalArgumentException) {
        println("[ERROR] Producto no válido: ${e.message} Corrija el precio e intente nuevamente.")
    }

    val pizza1 = Producto("Pepperoni Suprema", 12990.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni"))
    val pizza2 = Producto("Cuatro Quesos", 13990.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Queso Azul"))
    val pizza3 = Producto("Napolitana", 11990.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Tomate"))
    val pizza4 = Producto("Hawaiana", 12490.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Piña", "Jamón"))

    // 5- PEDIDOS: se evalúa el inventario ANTES de aceptar cada pedido
    val pedidosEntrantes = listOf(
        Pedido(101, cliente1, listOf(ItemPedido(pizza1, 1), ItemPedido(pizza2, 1), ItemPedido(pizza3, 1), ItemPedido(pizza4, 1)), EstadoPedido.EN_PREPARACION, null), // 4 pizzas para activar el descuento del 10%
        Pedido(102, cliente2, listOf(ItemPedido(pizza3, 2), ItemPedido(pizza4, 1)), EstadoPedido.LISTO, "Sin cebolla, por favor"),
        Pedido(103, cliente3, listOf(ItemPedido(pizza1, 6)), EstadoPedido.EN_PREPARACION), // pedirá más ingredientes de los disponibles
        Pedido(104, cliente1, listOf(ItemPedido(pizza2, 1)), EstadoPedido.LISTO)
    )

    val pedidosAceptados = mutableListOf<Pedido>()
    for (pedido in pedidosEntrantes) {
        println("\n--- [PASO 1] Verificando inventario para el pedido #${pedido.id} ---")
        val faltantes = verificarDisponibilidad(pedido, inventarioIngredientes)
        if (faltantes.isEmpty()) {
            try {
                for (item in pedido.items) {
                    prepararLote(item.producto.ingredientes, item.cantidad, inventarioIngredientes)
                }
                pedidosAceptados.add(pedido)
                println("Pedido #${pedido.id} aceptado e inventario actualizado.")
            } catch (e: IllegalStateException) {
                println("[ERROR] No se pudo preparar el pedido #${pedido.id}: ${e.message}")
            }
        } else {
            println("Pedido #${pedido.id} RECHAZADO. Ingredientes insuficientes: ${faltantes.keys.joinToString(", ")}")
            mostrarPasosReabastecimiento(faltantes)
        }
    }

    // Almacenando pedidos en un Arreglo (Array)
    val listaPedidosDelDia = pedidosAceptados.toTypedArray()

    // Cambio de estado de un pedido
    println("\n--- Actualizando estados ---")
    listaPedidosDelDia[1].cambiarEstado(EstadoPedido.EN_CAMINO)

    // 6- REPORTE DEL DÍA (for sobre el arreglo) Y AGRUPACIÓN CON FILTER
    println("\n--- [PASO 1] Reporte de pedidos del día ---")
    for (p in listaPedidosDelDia) {
        val pizzas = p.items.joinToString(", ") { "${it.producto.nombre} x${it.cantidad}" }
        println("Pedido #${p.id} | Cliente: ${p.cliente.nombre} | Pizzas: $pizzas | Total: ${precio(p.calcularTotalFinal())} | Estado: ${p.estado.texto}")
    }
    val pedidosPendientes = listaPedidosDelDia.filter { it.estado == EstadoPedido.EN_PREPARACION }
    val pedidosCompletados = listaPedidosDelDia.filter { it.estado != EstadoPedido.EN_PREPARACION } // Listo o En camino
    println("\n[Info de Sistema] Pedidos pendientes actuales en cola: ${pedidosPendientes.size}")
    println("[Info de Sistema] Pedidos completados (listos o en camino): ${pedidosCompletados.size}")

    // 7- RESUMEN Y EJECUCIÓN
    for (p in listaPedidosDelDia) {
        // Null Safety: operador ?: para las notas y find para asignar el repartidor de la zona del cliente
        val notaFinal = p.notas ?: "Sin notas"
        val repartidorAsignado = repartidoresDisponibles.find { it.zona == p.cliente.zona } ?: repartidoresDisponibles[0]

        println("\n=========================================")
        println("       RESUMEN OFICIAL DEL PEDIDO          ")
        println("===========================================")
        println("Pedido ID: #${p.id}")
        println("Cliente: ${p.cliente.nombre}")
        println("Contacto: Tel: ${p.cliente.validarTelefono()} | Correo: ${p.cliente.validarCorreo()}")
        println("Notas del pedido: $notaFinal")

        println("\n--- Detalle de Productos ---")
        for (item in p.items) {
            println("• ${item.producto.nombre} | Precio: ${precio(item.producto.precio)} | Cantidad: ${item.cantidad}")
            println("   ${item.producto.detallarProducto()}")

        }

        println("---------------------------------------")
        println("Subtotal: ${precio(p.calcularSubtotal())}")
        println("Impuestos (19% IVA): ${precio(p.calcularImpuestos())}")
        if (p.calcularDescuento() > 0){
            println("Descuento aplicado (10% por >3 pizzas): -${precio(p.calcularDescuento())}")
        } else {
            println("Descuento aplicado: ninguno")
        }
        println("TOTAL FINAL A PAGAR: ${precio(p.calcularTotalFinal())}")
        println("Estado del pedido: ${p.estado.texto}")
        println("Repartidor Asignado: ${repartidorAsignado.nombre} (Zona: ${repartidorAsignado.zona})")
        println("=======================================")

        // let: solo se notifica si el cliente tiene correo registrado
        p.cliente.correo?.let { correo ->
            println("[Notificación] Resumen del pedido #${p.id} enviado a $correo")
        }

        // Simular la entrega llamado al metodo del repartidor
        if (p.estado == EstadoPedido.EN_CAMINO) {
            repartidorAsignado.entregarPedido(p.id)
        }
    }

    // Inventario final
    println("\n--- Inventario final ---")
    for ((ingrediente, cantidad) in inventarioIngredientes) {
        println("$ingrediente: $cantidad")
    }
}
