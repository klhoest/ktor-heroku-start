package blog

import java.util.TreeSet

class VerdunAI(val solarSystem: TreeSet<WorkablePlanet>) {

    val target:WorkablePlanet = findTarget()!! // todo
    val requiredArmy:Int
        get() {
            var extraCare = 0
            if(target.colony.owner != WorkablePlanet.Guilde_du_Commerce) {
                extraCare = 10
            }
            return extraCare + target.enemyPop + target.empireFleetIncoming + (target.enemyCivilainNearby - target.rebelCivilianNearby).toInt() - target.rebelionFleetIncoming - sentFleet
        }
    var sentFleet = 0
    val overPopulatedPlanetList: ArrayList<PlanetRebel>
        get() {
            val result = ArrayList<PlanetRebel>()
            solarSystem
                    .filterIsInstance<PlanetRebel>()
                    .filterTo(result) { it.isOverpopulated }
            return result
        }

    fun findTarget(): WorkablePlanet? {
        var inspectPlanet:WorkablePlanet
        val it = solarSystem.descendingIterator()
        while (it.hasNext()) {
            inspectPlanet = it.next()
            if(!inspectPlanet.safe) {
                System.out.println("target : " + inspectPlanet.colony.id + " , interest = " + inspectPlanet.interest)
                return inspectPlanet
            }
        }
        System.out.println("no target found. skip this turn");
        return null
    }

    fun itsATrap() {
        returnJSON.fleets.clear()
    }

    fun mobilise() {
        var mobiliseOverpopulation = false
        for(inspectedPlanet in overPopulatedPlanetList) {
            if(requiredArmy<0)
                return
            sentFleet = inspectedPlanet.pourFrodon(target.colony.id, requiredArmy)
            mobiliseOverpopulation = true;
        }
        val it = solarSystem.iterator()
        while(requiredArmy > 0 && it.hasNext()) {
            val inspectPlanet = it.next()
            if(inspectPlanet is PlanetRebel) {
                inspectPlanet.pourFrodon(target.colony.id, requiredArmy)
            }
        }
        /*if(requiredArmy>0 && !mobiliseOverpopulation) {
            itsATrap();
        }*/
    }

    companion object {
        const val W = 2-1
        const val H = 2-1

        const val SCREEN_X = 1500.0
        const val SCREEN_Y = 700
    }
}