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
                val verdunAI = VerdunAI(solarSystem)
                verdunAI.mobilise()

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


class VerdunAI(val solarSystem: TreeSet<WorkablePlanet>) {

    val it = solarSystem.iterator();
    val target:WorkablePlanet
        get() = findTarget()
    val requiredArmy:Int
        get() = target.enemyPop + target.empireFleetIncoming + (target.enemyCivilainNearby).toInt() - target.rebelionFleetIncoming
    val overPopulatedPlanetList: ArrayList<PlanetRebel>
        get() {
            val result = ArrayList<PlanetRebel>()
            for(inspectPlanet in solarSystem) {
                if (inspectPlanet is PlanetRebel) {
                    if(inspectPlanet.isOverpopulated) {
                        result.add(inspectPlanet)
                    }
                }
            }
            return result
        }

    fun findTarget(): WorkablePlanet {
        while (it.hasNext()) {
            if (it.next() is PlanetRebel) {
                if((it.next() as PlanetRebel).threaten)
                    break
            } else {
                break
            }
        }
        return it.next()
    }

    fun itsATrap() {
        returnJSON.fleets.clear()
    }

    fun mobilise() {
        var mobiliseOverpopulation = false
        for(inspectedPlanet in overPopulatedPlanetList) {
            if(requiredArmy<0)
                return
            inspectedPlanet.pourFrodon(target.colony.id)
            mobiliseOverpopulation = true;
        }
        val rit = solarSystem.descendingIterator()
        while(requiredArmy > 0 && rit.hasNext()) {
            if(rit.next() is PlanetRebel) {
                (rit.next() as PlanetRebel).pourFrodon(target.colony.id)
            }
        }
        if(requiredArmy>0 && !mobiliseOverpopulation) {
            itsATrap();
        }
    }
}



open class WorkablePlanet(val colony: Planet, val laRebelion: List<Planet>, val lEmpire: List<Planet>, val rebelionFleet: List<Fleet>, val empireFleet: List<Fleet>) : Comparable<PlanetRebel> {

    val interest: Int
        get() = colony.gr!! - empireFleetIncoming/10 - enemyPop/10;
    val enemyPop: Int
        get() {
            if (colony.owner == ME) {
                return 0;
            } else {
                return colony.units!!;
            }
        }
    val remainingPlace: Int
        get() = colony.mu!! - colony.units!!
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

    // we add the add to make sure that each planet have a different comparable interest
    override fun compareTo(other: PlanetRebel) = interest * 100 + colony.id % 100

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
    val threaten: Boolean
        get() = (colony.units!! + rebelionFleetIncoming - rebelionFleetIncoming) <= 0
    val maxPop = (colony.mu!!-colony.gr!!)*2
    val isOverpopulated: Boolean
        get() = colony.units!! > maxPop
    val minPop: Int
        get() {

            val temp = min(maxPop ,enemyCivilainNearby.toInt() + empireFleetIncoming - rebelionFleetIncoming + 1)
            return max (temp, 1)
        }

    fun pourFrodon(targetId: Int) {
        if(sendableUnits >= 3) {
            returnJSON.fleets.add(FleetOrder(sendableUnits, source = colony.id, target = targetId))
        }
    }
}
