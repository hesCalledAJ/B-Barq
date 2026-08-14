package com.aliJafari.bbarq.utils

class BillIDNotFoundException : Exception()
class BillIDNot13Chars : Exception()
data class RequestUnsuccessful(val error : Exception? = null,val details : String = error?.message?:"No Response") : Exception()