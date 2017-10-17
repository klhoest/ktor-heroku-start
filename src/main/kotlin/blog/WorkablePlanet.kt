package blog

import java.lang.Integer.max
import java.lang.Integer.min

open class WorkablePlanet(val colony: Planet, laRebelion: List<Planet>, lEmpire: List<Planet>, val rebelionFleet: List<Fleet>, val empireFleet: List<Fleet>) : Comparable<WorkablePlanet> {

    val interest: Int = colony.gr!! + rebelionFleetIncoming/4 - empireFleetIncoming / 10 - enemyPop / 20;
    val enemyPop: Int
        get() {
            if (colony.owner == ME) {
                return 0;
            } else {
                return colony.units!!;
            }
        }
    val safe: Boolean = isSafe()
    val empireFleetIncoming: Int
        get() = getEnemyFleet()
    val rebelionFleetIncoming: Int
        get() = getRebelionFleet()
    val enemyCivilainNearby: Double = getNearbyCivilian(lEmpire)
    val rebelCivilianNearby: Double = getNearbyCivilian(laRebelion)

    fun getNearbyCivilian(fleetNationality: List<Planet>): Double {
        var result = 0.0
        for (inspectPlanet in fleetNationality) {
            if(inspectPlanet != this.colony)
                result += inspectPlanet.units!! / distance(colony, inspectPlanet)
        }
        return result
    }

    fun getRebelionFleet(): Int {
        var result = 0
        val incomingFleet = rebelionFleet.filter({ fleet -> fleet.to == colony.id })
        for (inpectFleet in incomingFleet) {
            result += inpectFleet.units ?: 0
        }
        return result
    }

    fun getEnemyFleet(): Int {
        val incomingFleet = empireFleet.filter({ fleet -> fleet.to == colony.id })
        //var FleetEnemyArray: Array<List<Fleet>> = Array(3, {ownerIndice -> incomingFleet.filter {  fleet -> fleet.owner == ownerIndice+ ME }})
        var result = incomingFleet.sumBy { it.units ?: 0 }
        when (colony.owner) {
            Guilde_du_Commerce -> result = max(result, enemyPop)
            ME -> result = result
            else -> {
                result = result + enemyPop
            }
        }
        return result
    }

    protected open fun isSafe():Boolean {
        return (rebelionFleetIncoming - empireFleetIncoming) > 0
    }

    // we add the add to make sure that each planet have a different comparable interest
    override fun compareTo(other: WorkablePlanet) = (interest * 100 + colony.id % 100) - (other.interest * 100 + other.colony.id % 100)

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
        fun distance(planet1: Planet, planet2: Planet): Double {
            val result = Math.hypot(planet1.x - planet2.x, planet1.y - planet2.y)
            if( result > 100000) {
                System.out.println("distance between planet " + planet1.id + " and planet " + planet2.id +" = "+ result)
                return 100000.0;
            }
            return result
        }
    }
}

interface AItoPlanet {
    fun getRebelInCluster(): Int
}

class PlanetRebel(colony: Planet, laRebelion: List<Planet>, lEmpire: List<Planet>, rebelionFleet: List<Fleet>, empireFleet: List<Fleet>)
    : WorkablePlanet(colony, laRebelion, lEmpire, rebelionFleet, empireFleet) {
    //var inpect: Planet;
    val sendableUnits: Int
        get() = Integer.max(colony.units!! - minPop, 0);
    var maxPop: Int = colony.mu!!-(colony.gr!!*2)
        get() = colony.mu!!-(colony.gr!!*2) - alreadySentFleet
    val isOverpopulated: Boolean
        get() = colony.units!! > maxPop
    var rebelInside: Int? = null
        get() {
            return aIToPlanet?.getRebelInCluster()
        }
    val minPop: Int
        get() {
            val temp = Integer.min(maxPop, /*enemyCivilainNearby.toInt()*/ max(empireFleetIncoming - rebelionFleetIncoming, 0) + alreadySentFleet + 1 - (aIToPlanet?.getRebelInCluster() ?: 0)/7 )
            return Integer.max(temp, 1)
        }
    var alreadySentFleet:Int = 0
    var aIToPlanet: AItoPlanet? = null

    override fun isSafe(): Boolean {
        return (rebelionFleetIncoming - empireFleetIncoming + colony.units!!) > 0
    }

    fun pourFrodon(targetId: Int, requiredFleet: Int, aiFleetOrders: ArrayList<FleetOrder>): Int {
        if(targetId == this.colony.id) {
            println("warnin: planet${targetId} tried to attack itself")
            return 0
        }
        var sentFleet = min(sendableUnits / 4, requiredFleet)
        if (sentFleet < 3) {
            if (sendableUnits < 3) {
                System.out.println("the sendable units are lower than 3")
                return 0
            } else {
                sentFleet = 3
            }
        }
        System.out.println("pourFrodon. send " + (sendableUnits) + "/" + this.colony.units + " from " + this.colony.id + " remains " + (requiredFleet - sendableUnits / 4))
        alreadySentFleet += sentFleet
        aiFleetOrders.add(FleetOrder(sentFleet, source = colony.id, target = targetId))
        return sentFleet
    }
}