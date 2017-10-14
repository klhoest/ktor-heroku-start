package blog

import java.lang.Integer.min

open class WorkablePlanet(val colony: Planet, val laRebelion: List<Planet>, val lEmpire: List<Planet>, val rebelionFleet: List<Fleet>, val empireFleet: List<Fleet>) : Comparable<WorkablePlanet> {

    val interest: Int = colony.gr!! - empireFleetIncoming/10 - enemyPop/10;
    val enemyPop: Int
        get() {
            if (colony.owner == ME) {
                return 0;
            } else {
                return colony.units!!;
            }
        }
    val safe: Boolean = isSafe()
    val remainingPlace: Int
        get() = colony.mu!! - colony.units!!
    val empireFleetIncoming: Int
        get() = getIncomingFleet(fleetNationality = empireFleet)
    val rebelionFleetIncoming: Int
        get() = getIncomingFleet(fleetNationality = rebelionFleet)
    val enemyCivilainNearby: Double = getNearbyCivilian(lEmpire)
    val rebelCivilianNearby: Double = getNearbyCivilian(laRebelion)

    protected fun getNearbyCivilian(fleetNationality: List<Planet>): Double {
        var result = 0.0
        for (inspectPlanet in fleetNationality) {
            if(inspectPlanet != this.colony)
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

    protected open fun isSafe():Boolean {
        return (rebelionFleetIncoming - empireFleetIncoming - enemyPop) > 0
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

class PlanetRebel(colony: Planet, laRebelion: List<Planet>, lEmpire: List<Planet>, rebelionFleet: List<Fleet>, empireFleet: List<Fleet>)
    : WorkablePlanet(colony, laRebelion, lEmpire, rebelionFleet, empireFleet) {
    //var inpect: Planet;
    val sendableUnits: Int
        get() = Integer.max(colony.units!! - minPop, 0);
    val maxPop = colony.mu!!-(colony.gr!!*2)
    val isOverpopulated: Boolean
        get() = colony.units!! > maxPop
    val minPop: Int
        get() {
            val temp = Integer.min(maxPop, /*enemyCivilainNearby.toInt()*/ + empireFleetIncoming - rebelionFleetIncoming + 1)
            return Integer.max(temp, 1)
        }
    var alreadySentFleet:Int = 0

    override fun isSafe():Boolean {
        return (rebelionFleetIncoming - empireFleetIncoming + colony.units!!) > 0
    }

    fun pourFrodon(targetId: Int, requiredFleet: Int): Int {
        var sentFleet = min(sendableUnits/4, requiredFleet)
        if(sentFleet < 3) {
            if(sendableUnits<3) {
                System.out.println("the sendable units are lower than 3")
                return 0
            } else {
                sentFleet = 3
            }
        }
        System.out.println("pourFrodon. send "+ (sendableUnits) + "/" + this.colony.units + " on innerTarget: " + targetId + "from "+ this.colony.id +" remains " + (requiredFleet-sendableUnits/4) )
        alreadySentFleet += sentFleet
        returnJSON.fleets.add(FleetOrder(sentFleet, source = colony.id, target = targetId))
        return sentFleet
    }
}