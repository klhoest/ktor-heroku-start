package blog

/*import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject*/
import blog.VerdunAI.Companion.H
import blog.VerdunAI.Companion.SCREEN_X
import blog.VerdunAI.Companion.SCREEN_Y
import blog.VerdunAI.Companion.W
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
import java.lang.Math.floorDiv
//import org.jetbrains.ktor.heroku.module
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
            returnJSON.fleets.clear()
            val inputBody: String = call.request.receiveContent().readText()
            var inputPojo: Pojo? = null
            try {
                System.out.println("received json :");
                System.out.println(inputBody)
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
                val solarSystemGrid = generateSolarSystemGrid(solarSystemPojo, laRebelionPojo, lEmpirePojo, rebelionFleet, empireFleet )

                for(x in 0 until W){
                    for(y in 0 until H) {
                        try {
                            val verdunAI = VerdunAI(solarSystemGrid[x][y])
                            verdunAI.mobilise()
                        } catch (e:NullPointerException) {
                            System.out.println("no target found" + e.message)
                        }
                    }
                }
            }
            //var sortedPlanet:List<Planet?>? = IA.sortPlanetByDistance(returnPojo.solarSystem[0]!! ,returnPojo.solarSystem)
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

fun generateSolarSystemGrid(solarSystemPojo: List<Planet>, laRebelionPojo: List<Planet>, lEmpirePojo: List<Planet>, rebelionFleet: List<Fleet>, empireFleet: List<Fleet>)
:Array<Array<TreeSet<WorkablePlanet>>>{

    val solarSystemGrid: Array<Array<TreeSet<WorkablePlanet>>> = Array<Array<TreeSet<WorkablePlanet>>>(W, { Array(H, {TreeSet<WorkablePlanet>()})})

    for (inspectPlanet in solarSystemPojo) {
        val x:Int = floorDiv(inspectPlanet.x.toInt(), (SCREEN_X/W))
        val y:Int = floorDiv(inspectPlanet.y.toInt(), (SCREEN_Y/H))
        try {
            System.out.println("adding id:" + inspectPlanet.id + " of owner: "+ inspectPlanet.owner + " with growing of : " + inspectPlanet.gr);
            if (inspectPlanet.owner == WorkablePlanet.ME) {
                solarSystemGrid[x][y].add(PlanetRebel(inspectPlanet, laRebelionPojo, lEmpirePojo, rebelionFleet, empireFleet))
            } else {
                solarSystemGrid[x][y].add(WorkablePlanet(inspectPlanet, laRebelionPojo, lEmpirePojo, rebelionFleet, empireFleet))
            }
        } catch (e:ClassCastException) {
            System.out.println("could not add id:" + inspectPlanet.id + " of owner: "+ inspectPlanet.owner + " with ");
        } catch (e:ArrayIndexOutOfBoundsException) {
            System.out.println("wrong grid positionning" + e.message)
        }
    }
    return solarSystemGrid
}

fun generatePojo(inputBody: String): Pojo {
    val gson = Gson()
    return gson.fromJson<Pojo>(inputBody, Pojo::class.java)
}



