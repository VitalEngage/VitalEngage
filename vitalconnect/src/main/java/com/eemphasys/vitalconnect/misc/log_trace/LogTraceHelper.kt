package com.eemphasys.vitalconnect.misc.log_trace

import android.content.Context
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.eemphasys_enterprise.commonmobilelib.UtilityDataModel

object LogTraceHelper {
    fun debug(
        context: Context?,
        logData: String?,
        logModule: String?,
        utilityDataModel: UtilityDataModel
    ) {
        try {
            EETLog.debug(
                context,
                logData,
                logModule,
                utilityDataModel
            )
        } catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            );
        }
    }

    fun error(
        context: Context?,
        logData: String?,
        logModule: String?,
        utilityDataModel: UtilityDataModel
    ) {
        try {
            EETLog.error(
                context,
                logData,
                logModule,
                utilityDataModel
            )
        } catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            );
        }
    }

    fun warn(
        context: Context?,
        logData: String?,
        logModule: String?,
        utilityDataModel: UtilityDataModel
    ) {
        try {
            EETLog.warn(
                context,
                logData,
                logModule,
                utilityDataModel
            )
        } catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            );
        }
    }

    fun info(
        context: Context?,
        logData: String?,
        logModule: String?,
        utilityDataModel: UtilityDataModel
    ) {
        try {
            EETLog.info(
                context,
                logData,
                logModule,
                utilityDataModel
            )
        } catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            );
        }
    }

    fun trace(
        context: Context?,
        logData: String?,
        logModule: String?,
        utilityDataModel: UtilityDataModel
    ) {
        try {
            EETLog.trace(
                context,
                logData,
                logModule,
                utilityDataModel
            )
        } catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            );
        }
    }
}