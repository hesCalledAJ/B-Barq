package com.aliJafari.bbarq.data.repository

import android.content.Context
import com.aliJafari.bbarq.data.local.AuthStorage
import com.aliJafari.bbarq.data.model.Outage
import com.aliJafari.bbarq.data.model.Place
import com.aliJafari.bbarq.utils.BillIDNot13Chars
import com.aliJafari.bbarq.utils.BillIDNotFoundException
import com.aliJafari.bbarq.utils.RequestUnsuccessful
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import saman.zamani.persiandate.PersianDate
import saman.zamani.persiandate.PersianDateFormat
import java.util.concurrent.TimeUnit

class OutageRepository(val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    fun sendRequest(billId: String, onResult: (List<Outage>) -> Unit) {
        onResult(fetchOutages(billId))
    }

    fun fetchOutages(billId: String): List<Outage> {
        if (billId.length!=13) throw BillIDNot13Chars()
        val fromDate = PersianDate()
        val toDate = PersianDate().addDays(5)
        val dateFormat = PersianDateFormat("Y/m/d")
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val jsonBody = """
    {
        "bill_id": "$billId",
        "from_date": "${dateFormat.format(fromDate)}",
        "to_date": "${dateFormat.format(toDate)}"
    }
""".trimIndent()

        val requestBody = jsonBody.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("https://uiapi.saapa.ir/api/ebills/PlannedBlackoutsReport")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${AuthStorage(context).getToken()}")
            .addHeader("Accept", "application/json, text/plain, */*")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body.string()
                val json = JSONObject(body)
                return parseApiResponse(json.toString()).data.map {
                    it.toOutage(billId)
                }
            } else {
                throw RequestUnsuccessful(null,response.message)
            }
        } catch (e: BillIDNotFoundException) {
            e.printStackTrace()
            throw BillIDNotFoundException()
        }catch (e: Exception) {
            e.printStackTrace()
            throw RequestUnsuccessful(e)
        }
    }

    fun fetchOutagesForPlaces(places: List<Place>): List<PlaceOutage> =
        places.flatMap { place ->
            fetchOutages(place.billId).map { outage ->
                PlaceOutage(place = place, outage = outage)
            }
        }

    private fun parseApiResponse(jsonString: String): ApiResponseModel {
        val jsonObject = JSONObject(jsonString)
        val outagesList = mutableListOf<ApiResponseOutageModel>()
        val data = jsonObject.getJSONArray("data")
        for (i in 0 until data.length()){
            data.getJSONObject(i).apply {
                outagesList.add(
                    ApiResponseOutageModel(
                        regDate = getString("reg_date"),
                        register = getString("registrar"),
                        reasonOutage = getString("reason_outage"),
                        outageDate = getString("outage_date"),
                        outageTime = getString("outage_time"),
                        outageStartTime = getString("outage_start_time"),
                        outageStopTime = getString("outage_stop_time"),
                        isPlanned = getBoolean("is_planned"),
                        address = getString("address"),
                        outageAddress = getString("outage_address"),
                        city = getInt("city"),
                        outageNumber = getInt("outage_number"),
                        trackingCode = getInt("tracking_code")
                    )
                )
            }
        }
        return ApiResponseModel(
            timeStamp = jsonObject.getString("TimeStamp"),
            status = jsonObject.getInt("status"),
            sessionKey = jsonObject.getString("SessionKey"),
            message = jsonObject.getString("message"),
            data = outagesList,
            error = jsonObject.get("error")

        )
    }
}
data class PlaceOutage(
    val place: Place,
    val outage: Outage
)
data class ApiResponseModel(
    val timeStamp: String,
    val status: Int,
    val sessionKey: String,
    val message: String,
    val data: List<ApiResponseOutageModel>,
    val error : Any?
)
data class ApiResponseOutageModel(
    val regDate : String,
    val register : String,
    val reasonOutage: String,
    val outageDate: String,
    val outageTime: String,
    val outageStartTime: String,
    val outageStopTime: String,
    val isPlanned: Boolean,
    val address: String,
    val outageAddress: String,
    val city : Int,
    val outageNumber: Int,
    val trackingCode : Int
)
fun ApiResponseOutageModel.toOutage(billId:String) = Outage(outageNumber,reasonOutage,outageDate,outageStartTime,outageStopTime,billId,address)
