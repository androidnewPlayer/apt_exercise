package com.ray.networkapt

class BaseHttpResult<T>(var code: Int = -1, var message: String = "", var data: T? = null)