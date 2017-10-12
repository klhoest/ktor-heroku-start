package blog

import com.google.gson.annotations.SerializedName

data class Pojo(
        @SerializedName("planets") val planets: List<Planet?>,
        @SerializedName("fleets") val fleets: List<Fleet?>?,
        @SerializedName("config") val config: Config
)

data class Planet(
        @SerializedName("id") val id: Int, //1
        @SerializedName("x") var x: Double, //20.55
        @SerializedName("y") val y: Double, //20.77
        @SerializedName("owner") val owner: Int?, //1
        @SerializedName("units") val units: Int?, //34
        @SerializedName("mu") val mu: Int?, //100
        @SerializedName("gr") val gr: Int?, //2
        @SerializedName("classe") val classe: String?, //M
        @SerializedName("tr") val tr: Int? //-1 null
)

data class Config(
        @SerializedName("id") val id: Int, //234
        @SerializedName("turn") val turn: Int, //2
        @SerializedName("maxTurn") val maxTurn: Int //200
)

data class Fleet(
        @SerializedName("owner") val owner: Int?, //1
        @SerializedName("units") val units: Int?, //15
        @SerializedName("from") val from: Int?, //1
        @SerializedName("to") val to: Int?, //2
        @SerializedName("turns") val turns: Int?, //12
        @SerializedName("left") val left: Int? //2
)