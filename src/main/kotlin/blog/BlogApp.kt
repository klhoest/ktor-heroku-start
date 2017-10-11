package blog

/*import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject*/
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.jetbrains.ktor.netty.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.application.*
//import org.jetbrains.ktor.features.CallLogging
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.gson.GsonSupport
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.response.*
import org.jetbrains.ktor.content.readText
import org.jetbrains.ktor.features.CallLogging
//import org.jetbrains.ktor.heroku.module
import java.lang.Integer.max
import java.lang.Integer.min


data class ReturnJSON(var fleets: ArrayList<FleetOrder> = ArrayList(), var terraformings: ArrayList<TeraformOrder> = ArrayList())
data class FleetOrder(val units: Int, val source: Int, val target: Int)
data class TeraformOrder(var planet: Int)

public val returnJSON = ReturnJSON()


fun Application.module() {
    install(DefaultHeaders)
    //install(Compression)
    install(CallLogging)

    install(GsonSupport) {
        setPrettyPrinting()
    }

    routing {

        get("/") {
            call.respond(returnJSON)
        }

        post("/") {
            val inputBody: String = call.request.receiveContent().readText()
            var inputPojo: Pojo? = null
            try {
                inputPojo = generatePojo(inputBody)
            } catch (e: Exception) {
                call.respond(returnJSON)
            }
            if (inputPojo != null) {
                val solarSystem = inputPojo.planets.filterNotNull()
                val laRebelion = solarSystem.filter { planet: Planet -> planet.owner!! == PlanetIA.ME; }
                val lEmpire = solarSystem.filter { planet: Planet -> planet.owner!! != PlanetIA.ME; }

                val allFleet = inputPojo.fleets!!.filterNotNull()
                val rebelionFleet = allFleet.filter { fleet -> fleet.owner!! == PlanetIA.ME; }
                val empireFleet = allFleet.filter { fleet -> fleet.owner!! != PlanetIA.ME; }
                try {
                    for (colony in laRebelion) {
                        var planetIA = PlanetIA(colony, laRebelion, lEmpire, rebelionFleet, empireFleet)
                    }
                } catch (e: Exception) {
                    call.respond(returnJSON)
                }
            }
            //var sortedPlanet:List<Planet?>? = IA.sortPlanetByDistance(returnPojo.solarSystem[0]!! ,returnPojo.solarSystem)
            call.respond(returnJSON)

        }
    }

}

fun main(args: Array<String>) {
    val port = Integer.valueOf(System.getenv("PORT"))
    embeddedServer(Netty, port= port, watchPaths = listOf("BlogAppKt"), module = Application::module).start()
}

fun generatePojo(inputBody: String): Pojo {
    val gson = Gson()
    return gson.fromJson<Pojo>(inputBody, Pojo::class.java)
}

class PlanetIA(val colony: Planet, val laRebelion: List<Planet>, val lEmpire: List<Planet>, val rebelionFleet:List<Fleet>, val empireFleet:List<Fleet>) {
    //var inpect: Planet;
    val sendableUnits: Int
        get() = max(colony.units!! - minPop, 0);
    val remainingPlace: Int
        get() = colony.mu!! - colony.units!!
    val threaten: Boolean
        get() = (colony.units!! + rebelionFleetIncoming -rebelionFleetIncoming) <= 0
    val minPop: Int
        get() = enemyCivilainNearby.toInt() + empireFleetIncoming - rebelionFleetIncoming + 1 //warning: can be negative
    val empireFleetIncoming:Int
        get() = getIncomingFleet(fleetNationality=empireFleet)
    val rebelionFleetIncoming:Int
        get() = getIncomingFleet(fleetNationality = rebelionFleet)
    val enemyCivilainNearby:Double
        get() {
            var result = 0.0
            for(inspectPlanet in lEmpire) {
                result += inspectPlanet.units!!/distance(colony, inspectPlanet)
            }
            return result
        }

    init {
        for(deathStar in lEmpire) {
            returnJSON.fleets.add(FleetOrder(min(3, sendableUnits), source = colony.id, target = deathStar.id))
        }

    }

    private fun getIncomingFleet(fleetNationality:List<Fleet>): Int {
        var result = 0
        val incomingFleet = fleetNationality.filter({ fleet -> fleet.to == colony.id })
        for(inpectFleet in incomingFleet) {
            result += inpectFleet.units ?: 0
        }
        return result
    }

    companion object {
        //__Player
        const val Guilde_du_Commerce: Int = 0
        const val ME: Int = 1

        @JvmStatic
        fun sortPlanetByDistance(from: Planet, planets: List<Planet?>): List<Planet?>? {
            var filterplanets = planets.filterNotNull();
            filterplanets = filterplanets.filter { planet -> planet?.owner != 1 }
            return filterplanets.sortedBy { to: Planet -> PlanetIA.distance(from, to) }
        }

        @JvmStatic
        fun distance(planet1: Planet, planet2: Planet): Double = Math.hypot(planet1.x - planet2.x, planet1.y - planet2.y)
    }
}
