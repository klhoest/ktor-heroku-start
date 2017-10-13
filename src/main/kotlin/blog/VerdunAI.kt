package blog

import java.util.TreeSet

class VerdunAI(val solarSystem: TreeSet<WorkablePlanet>) {

    val target:WorkablePlanet = findTarget()!! // todo
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

    fun findTarget(): WorkablePlanet? {
        var inspectPlanet:WorkablePlanet?
        val it = solarSystem.descendingIterator()
        while (it.hasNext()) {
            inspectPlanet = it.next()
            if (inspectPlanet is PlanetRebel) {
                if(inspectPlanet.threaten) {
                    System.out.println("target : " + inspectPlanet.colony.id + " , interest = " + inspectPlanet.interest)
                    return inspectPlanet
                }
            } else { //if planet does not belong to me
                System.out.println("target : " + inspectPlanet.colony.id + " , interest = " + inspectPlanet.interest)
                return inspectPlanet
            }
        }
        return null
        /*for(inspectPlanet in solarSystem) {
            if (inspectPlanet is PlanetRebel) {
                if (inspectPlanet.threaten) {
                    System.out.println("target : " + inspectPlanet.colony.id + " , interest = " + inspectPlanet.interest)
                    return inspectPlanet
                }
            } else {
                System.out.println("target : " + inspectPlanet.colony.id + " , interest = " + inspectPlanet.interest)
                return inspectPlanet
            }
        }
        return null*/
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
        val it = solarSystem.iterator()
        while(requiredArmy > 0 && it.hasNext()) {
            val inspectPlanet = it.next()
            if(inspectPlanet is PlanetRebel) {
                inspectPlanet.pourFrodon(target.colony.id)
            }
        }
        /*if(requiredArmy>0 && !mobiliseOverpopulation) {
            itsATrap();
        }*/
    }
}