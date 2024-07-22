package com.eemphasys.vitalconnect.misc.log_trace

import android.content.Context
import android.util.Log
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.SessionHelper
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants
import com.eemphasys_enterprise.commonmobilelib.UtilityDataModel

object LogTraceConstants {

    var chatappmodel: String = "VitalConnect"

    fun logDetails(
        exceptionMessage: Exception?,
        log_type: String?,
        log_severity: String?
    ): String? {
        val logInfoBuilder = StringBuilder()

        try {
            val logInfoData =
                LogConstants.logDetails(exceptionMessage!!, log_type!!, log_severity!!)

            logInfoBuilder.append(logInfoData ?: "")
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

            logInfoBuilder.append("")
        }

        return logInfoBuilder.toString()
    }

    fun crashDetails(
        exceptionMessage: Throwable?,
        stacktrace: String?,
        log_type: String?,
        log_severity: String?
    ): String? {
        val excMsgBuilder = StringBuilder()

        try {
            val excInfoData =
                LogConstants.crashDetails(exceptionMessage, stacktrace, log_type!!, log_severity!!)

            excMsgBuilder.append(excInfoData ?: "")
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

            excMsgBuilder.append("")
        }

        return excMsgBuilder.toString()
    }

    fun traceDetails(
        stackTraceElements: Array<StackTraceElement>?,
        traceMessage: String?,
        log_type: String?,
        log_severity: String?
    ): String? {
        val traceMsgBuilder = StringBuilder()

        try {
            val traceInfoData =
                LogConstants.traceDetails(
                    stackTraceElements!!,
                    traceMessage!!,
                    log_type!!,
                    log_severity!!
                )

            traceMsgBuilder.append(traceInfoData ?: "")
        } catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX,
                LogTraceConstants.getUtilityData(
                    SessionHelper.appContext!!
                )!!
            );

            traceMsgBuilder.append("")
        }

        return traceMsgBuilder.toString()
    }

    fun getUtilityData(
        context: Context
    ): UtilityDataModel? {
        var utilityDataModel: UtilityDataModel? = UtilityDataModel(
            "EForms",
            "",
            "",
            "",
            "",
            "",
            context!!.resources.getString(R.string.app_name),
            context.getExternalFilesDir(null)!!.absolutePath + "/Logs"
        )

        try {
            /*if (CheckListTabsModel.isChecklistTabModelInitialized) {
                utilityDataModel = UtilityDataModel(
                    if (CheckListTabsModel.traceModule != null && !CheckListTabsModel.traceModule.equals(
                            ""
                        )
                    ) {
                        CheckListTabsModel.traceModule!!
                    } else {
                        ""
                    },
                    if (CheckListTabsModel.empNameWithNo != null && !CheckListTabsModel.empNameWithNo.equals(
                            ""
                        )
                    ) {
                        CheckListTabsModel.empNameWithNo!!
                    } else {
                        ""
                    },
                    if (CheckListTabsModel.Company != null && !CheckListTabsModel.Company.equals(
                            ""
                        )
                    ) {
                        CheckListTabsModel.Company!!
                    } else {
                        ""
                    },
                    if (CheckListTabsModel.serviceCenterKey != null && !CheckListTabsModel.serviceCenterKey.equals(
                            ""
                        )
                    ) {
                        CheckListTabsModel.serviceCenterKey!!
                    } else {
                        ""
                    },
                    if (CheckListTabsModel.baseURL != null && !CheckListTabsModel.baseURL.equals(
                            ""
                        )
                    ) {
                        CheckListTabsModel.baseURL!!
                    } else {
                        ""
                    },
                    if (CheckListTabsModel.appVersion != null && !CheckListTabsModel.appVersion.equals(
                            ""
                        )
                    ) {
                        CheckListTabsModel.appVersion!!
                    } else {
                        ""
                    },
                    if (CheckListTabsModel.appName != null && !CheckListTabsModel.appName.equals(
                            ""
                        )
                    ) {
                        CheckListTabsModel.appName!!
                    } else {
                        ""
                    },
                    if (CheckListTabsModel.logsDirectory != null && !CheckListTabsModel.logsDirectory.equals(
                            ""
                        )
                    ) {
                        CheckListTabsModel.logsDirectory!!
                    } else {
                        ""
                    }
                )
                Log.d("LogTraceConstants",utilityDataModel.LogsDirectory)

            }*/

        } catch (e: Exception) {
            e.printStackTrace()
            EETLog.error(
                SessionHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX,
                utilityDataModel!!
            );
        }

        return utilityDataModel!!
    }

}