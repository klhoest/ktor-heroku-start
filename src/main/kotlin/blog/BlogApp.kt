package blog

/*import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject*/
import com.google.gson.Gson
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
import java.util.TreeSet


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
                val solarSystemPojo = inputPojo.planets.filterNotNull()

                val laRebelionPojo = solarSystemPojo.filter { planet: Planet -> planet.owner!! == WorkablePlanet.ME; }
                val lEmpirePojo = solarSystemPojo.filter { planet: Planet -> planet.owner!! != WorkablePlanet.ME; }

                val allFleet = inputPojo.fleets!!.filterNotNull()
                val rebelionFleet = allFleet.filter { fleet -> fleet.owner!! == WorkablePlanet.ME; }
                val empireFleet = allFleet.filter { fleet -> fleet.owner!! != WorkablePlanet.ME; }

                val solarSystem = TreeSet<WorkablePlanet>()
                for (inspectPlanet in solarSystemPojo) {
                    if (inspectPlanet.owner == WorkablePlanet.ME) {
                        solarSystem.add(PlanetRebel(inspectPlanet, laRebelionPojo, lEmpirePojo, rebelionFleet, empireFleet))
                    } else {
                        solarSystem.add(WorkablePlanet(inspectPlanet, laRebelionPojo, lEmpirePojo, rebelionFleet, empireFleet))
                    }
                }
                val it = solarSystem.iterator();
                val target = findTarget(it);
                for (inspectPlanet in solarSystem) {
                    if(inspectPlanet is PlanetRebel) {
                        inspectPlanet.pourFrodon(target)
                    }
                }

                call.respond(returnJSON)
            }
            //var sortedPlanet:List<Planet?>? = IA.sortPlanetByDistance(returnPojo.solarSystem[0]!! ,returnPojo.solarSystem)
            call.respond(returnJSON)

        }
    }

}

fun main(args: Array<String>) {
    val port = Integer.valueOf(System.getenv("PORT"))
    embeddedServer(Netty, port = port, watchPaths = listOf("BlogAppKt"), module = Application::module).start()
}

fun generatePojo(inputBody: String): Pojo {
    val gson = Gson()
    return gson.fromJson<Pojo>(inputBody, Pojo::class.java)
}

fun findTarget(it: Iterator<WorkablePlanet>): Int {
    val target: Int
    while (it.hasNext()) {
        if (it.next() is PlanetRebel) {
            if((it.next() as PlanetRebel).threaten)
                break
        } else {
            break
        }
    }
    target = it.next().colony.id
    return target
}

open class WorkablePlanet(val colony: Planet, val laRebelion: List<Planet>, val lEmpire: List<Planet>, val rebelionFleet: List<Fleet>, val empireFleet: List<Fleet>) : Comparable<PlanetRebel> {

    val empireFleetIncoming: Int
        get() = getIncomingFleet(fleetNationality = empireFleet)
    val rebelionFleetIncoming: Int
        get() = getIncomingFleet(fleetNationality = rebelionFleet)
    val enemyCivilainNearby: Double
        get() {
            var result = 0.0
            for (inspectPlanet in lEmpire) {
                result += inspectPlanet.units!! / distance(colony, inspectPlanet)
            }
            return result
        }

    protected fun getIncomingFleet(fleetNationality: List<Fleet>): Int {
        var result = 0
        val incomingFleet = fleetNationality.filter({ fleet -> fleet.to == colony.id })
        for (inpectFleet in incomingFleet) {
            result += inpectFleet.units ?: 0
        }
        return result
    }

    override fun compareTo(other: PlanetRebel): Int {
        return (colony.gr!! - other.colony.gr!!) * 100 + colony.id % 100 // we add the add to make sure that each planet have a different comparable value
    }

    companion object {
        //__Player
        const val Guilde_du_Commerce: Int = 0
        const val ME: Int = 1

        /*@JvmStatic
        fun sortPlanetByDistance(from: Planet, planets: List<Planet?>): List<Planet?>? {
            var filterplanets = planets.filterNotNull();
            filterplanets = filterplanets.filter { planet -> planet?.owner != 1 }
            return filterplanets.sortedBy { to: Planet -> PlanetRebel.distance(from, to) }
        }*/

        @JvmStatic
        fun distance(planet1: Planet, planet2: Planet): Double = Math.hypot(planet1.x - planet2.x, planet1.y - planet2.y)
    }
}

class PlanetRebel(colony: Planet, laRebelion: List<Planet>, lEmpire: List<Planet>, rebelionFleet: List<Fleet>, empireFleet: List<Fleet>)
    : WorkablePlanet(colony, laRebelion, lEmpire, rebelionFleet, empireFleet) {
    //var inpect: Planet;
    val sendableUnits: Int
        get() = max(colony.units!! - minPop, 0);
    val remainingPlace: Int
        get() = colony.mu!! - colony.units!!
    val threaten: Boolean
        get() = (colony.units!! + rebelionFleetIncoming - rebelionFleetIncoming) <= 0
    val minPop: Int
        get() = enemyCivilainNearby.toInt() + empireFleetIncoming - rebelionFleetIncoming + 1 //warning: can be negative


    init {
        for (deathStar in lEmpire) {

        }

    }

    override fun compareTo(other: PlanetRebel): Int {
        return colony.gr!! - other.colony.gr!!
    }

    fun pourFrodon(target: Int) {
        if(sendableUnits >= 3) {
            returnJSON.fleets.add(FleetOrder(sendableUnits, source = colony.id, target = target))
        }
    }
}
