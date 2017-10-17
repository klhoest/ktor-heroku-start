package blog

import com.google.gson.Gson
import org.jetbrains.ktor.netty.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.gson.GsonSupport
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.response.*
import org.jetbrains.ktor.content.readText
import org.jetbrains.ktor.features.CallLogging
import org.nield.kotlinstatistics.Centroid
import org.nield.kotlinstatistics.multiKMeansCluster


data class ReturnJSON(var fleets: ArrayList<FleetOrder> = ArrayList(), var terraformings: ArrayList<TeraformOrder> = ArrayList())
data class FleetOrder(val units: Int, val source: Int, val target: Int)
data class TeraformOrder(var planet: Int)

val returnJSON = ReturnJSON()

fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging)

    install(GsonSupport) {
        setPrettyPrinting()
    }

    routing {

        get("/") {
            call.respond(returnJSON)
        }

        post("/") {
            returnJSON.fleets.clear()
            val inputBody: String = call.request.receiveContent().readText()
            var inputPojo: Pojo? = null
            try {
                System.out.println("______________received request________________");
                //System.out.println(inputBody)
                inputPojo = generatePojo(inputBody)
            } catch (e: Exception) {
                call.respond(returnJSON)
            }
            if (inputPojo != null) {

                val galaxy = Galaxy(inputPojo)
                val clusters = galaxy.generateClusters()
                val AIList = ArrayList<VerdunAI>(clusters.size)
                clusters.forEach { cluster ->
                    AIList.add(VerdunAI(cluster, AIList));
                }

                AIList.forEach { AI ->
                    AI.sortRebelPlanets.forEach { inpectPlanet ->
                        inpectPlanet.aIToPlanet = AI
                    }
                    AI.constructTargeting()
                }

                AIList.forEachIndexed { index, verdunAI ->
                    println("CENTROID: $index")
                    try {
                        verdunAI.print()
                        verdunAI.inerTarget?.mobilise()
                        verdunAI.outerTarget?.mobilise()
                    } catch (e:NullPointerException) {
                        System.out.println("no target found" + e.message)
                    }
                }

            }
            call.respond(returnJSON)

        }
    }

}

fun main(args: Array<String>) {
    try {
        val port = Integer.valueOf(System.getenv("PORT"))
        embeddedServer(Netty, port = port, watchPaths = listOf("BlogAppKt"), module = Application::module).start()
    } catch (e:NumberFormatException) {
        embeddedServer(Netty, port = 8080, watchPaths = listOf("BlogAppKt"), module = Application::module).start()
    }
}

fun generatePojo(inputBody: String): Pojo {
    val gson = Gson()
    return gson.fromJson<Pojo>(inputBody, Pojo::class.java)
}

class Galaxy(inputPojo: Pojo) {
    val solarSystemPojo = inputPojo.planets.filterNotNull()

    val laRebelionPojo = solarSystemPojo.filter { planet: Planet -> planet.owner!! == WorkablePlanet.ME; }
    val lEmpirePojo = solarSystemPojo.filter { planet: Planet -> planet.owner!! != WorkablePlanet.ME; }

    val allFleet = inputPojo.fleets!!.filterNotNull()
    val rebelionFleet = allFleet.filter { fleet -> fleet.owner!! == WorkablePlanet.ME; }
    val empireFleet = allFleet.filter { fleet -> fleet.owner!! != WorkablePlanet.ME; }

    fun generateClusters(): List<Centroid<WorkablePlanet>> {
        val wSolarSystem: List<WorkablePlanet?> = solarSystemPojo.map { inspectPlanet ->
            planetFactory(inspectPlanet)
        }
        val clusters = wSolarSystem.filterNotNull().multiKMeansCluster(k = 5,
                maxIterations = 1000,
                trialCount = 50,
                xSelector = { it.colony.x },
                ySelector = { it.colony.y }
        )
        return clusters;
    }

    private fun planetFactory(inspectPlanet: Planet): WorkablePlanet? {
        try {
            //System.out.println("adding id:" + inspectPlanet.id + " of owner: " + inspectPlanet.owner + " with growing of : " + inspectPlanet.gr);
            if (inspectPlanet.owner == WorkablePlanet.ME) {
                return PlanetRebel(inspectPlanet, laRebelionPojo, lEmpirePojo, rebelionFleet, empireFleet)
            } else {
                return WorkablePlanet(inspectPlanet, laRebelionPojo, lEmpirePojo, rebelionFleet, empireFleet)
            }
        } catch (e: ClassCastException) {
            System.out.println("could not add id:" + inspectPlanet.id + " of owner: " + inspectPlanet.owner + " with ");
            return null
        }
    }
}



