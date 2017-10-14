package blog

import org.nield.kotlinstatistics.Centroid
import java.util.*

class VerdunAI(val localCluster: Centroid<WorkablePlanet>,val AIList: List<VerdunAI>) {

    var solarSystemCell = localCluster.points.sortedDescending()
    var orderRebelPlanets = solarSystemCell.filterIsInstance<PlanetRebel>()
    var rebelInside: Int = 0
        get() {
            var result = 0
            for (inspectPlanet in solarSystemCell) {
                result += inspectPlanet.rebelionFleetIncoming
                if (inspectPlanet is PlanetRebel)
                    result += inspectPlanet.colony.units!!
            }
            return result
        }
    val innerTarget: WorkablePlanet = findInnerTarget()!! // todo
    val requiredArmy: Int
        get() {
            var extraCare = 0
            if (innerTarget.colony.owner != WorkablePlanet.Guilde_du_Commerce) {
                extraCare = 10
            }
            return extraCare + innerTarget.enemyPop + innerTarget.empireFleetIncoming + (innerTarget.enemyCivilainNearby - innerTarget.rebelCivilianNearby).toInt() - innerTarget.rebelionFleetIncoming - sentFleet
        }
    var sentFleet = 0
    val overPopulatedPlanetList: ArrayList<PlanetRebel>
        get() {
            val result = ArrayList<PlanetRebel>()
            orderRebelPlanets
                    .filterTo(result) { it.isOverpopulated }
            return result
        }

    fun print() {
        solarSystemCell.forEach {
            var extraTab = "\t"
            if (it.colony.owner == WorkablePlanet.ME) {
                extraTab = "\tX "
            }
            println(extraTab + "planet${it.colony.id}, of owner ${it.colony.owner}, interset:${it.interest}, gr:${it.colony.gr}")
        }
    }

    fun findInnerTarget(): WorkablePlanet? {
        var inspectPlanet: WorkablePlanet
        val it = solarSystemCell.iterator()
        while (it.hasNext()) {
            inspectPlanet = it.next()
            if (!inspectPlanet.safe) {
                System.out.println("innerTarget : " + inspectPlanet.colony.id + " , interest = " + inspectPlanet.interest)
                return inspectPlanet
            }
        }
        System.out.println("no innerTarget found. skip this turn");
        return null
    }

    fun findOutterTarget(): WorkablePlanet {
        var cellTargets = TreeMap<Double, WorkablePlanet>()
        AIList.forEach { inspectAI ->
            val target = inspectAI.innerTarget
            val key = (target.interest - Math.hypot(localCluster.center.x - target.colony.x, localCluster.center.y - target.colony.y)/40)*-1
            cellTargets.put(key, target)
        }
        val it = cellTargets.iterator()
        return cellTargets.firstEntry().value
    }

    fun itsATrap() {
        returnJSON.fleets.clear()
    }

    fun mobilise() {
        var mobiliseOverpopulation = false
        for (inspectedPlanet in overPopulatedPlanetList) {
            if (requiredArmy < 0)
                return
            sentFleet = inspectedPlanet.pourFrodon(innerTarget.colony.id, requiredArmy)
            mobiliseOverpopulation = true;
        }
        val it = orderRebelPlanets.iterator()
        while (requiredArmy > 0 && it.hasNext()) {
            sentFleet += it.next().pourFrodon(innerTarget.colony.id, requiredArmy)
        }
        /*if(requiredArmy>0 && !mobiliseOverpopulation) {
            itsATrap();
        }*/
    }
}