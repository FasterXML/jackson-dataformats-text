package perf;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO representing data from form F-5500
 */
public class F5500Entry
{
    // 1 - 10
    @JsonProperty("ACK_ID") public String ackId;
    @JsonProperty("SF_PLAN_YEAR_BEGIN_DATE") public String sfPlanYearBeginDate;
    @JsonProperty("SF_TAX_PRD") public String sfTaxPrd;
    @JsonProperty("SF_PLAN_ENTITY_CD") public String sfPlanEntityCD;
    @JsonProperty("SF_INITIAL_FILING_IND") public String sfInitialFilingInd;
    @JsonProperty("SF_AMENDED_IND") public String sfAmendedInd;
    @JsonProperty("SF_FINAL_FILING_IND") public String sfFinalFilingInd;
    @JsonProperty("SF_SHORT_PLAN_YR_IND") public String sfShortPlanYearInd;
    @JsonProperty("SF_5558_APPLICATION_FILED_IND") public String sf5558ApplicationFiledInd;
    @JsonProperty("SF_EXT_AUTOMATIC_IND") public String sfExtAutomaticInd;
    
    // 11 - 20
    @JsonProperty("SF_DFVC_PROGRAM_IND") public String sfDFVCProgramInd;
    @JsonProperty("SF_EXT_SPECIAL_IND") public String sfExtSpecialInd;
    @JsonProperty("SF_EXT_SPECIAL_TEXT") public String sfExtSpecialText;
    @JsonProperty("SF_PLAN_NAME") public String sfPlanName;
    @JsonProperty("SF_PLAN_NUM") public long sfPlanNumber;
    @JsonProperty("SF_PLAN_EFF_DATE") public String sfPlanEffectiveDate;
    @JsonProperty("SF_SPONSOR_NAME") public String sfSponsorName;
    @JsonProperty("SF_SPONSOR_DFE_DBA_NAME") public String sfSponsorDFEDBAName;
    @JsonProperty("SF_SPONS_US_ADDRESS1") public String sfSponsorUSAddress1;
    @JsonProperty("SF_SPONS_US_ADDRESS2") public String sfSponsorUSAddress2;

    // 21 - 30
    @JsonProperty("SF_SPONS_US_CITY") public String sfSponsorUSCity;
    @JsonProperty("SF_SPONS_US_STATE") public String sfSponsorUSState;
    @JsonProperty("SF_SPONS_US_ZIP") public String sfSponsorUSZip;
    @JsonProperty("SF_SPONS_FOREIGN_ADDRESS1") public String sfSponsorForeignAddress1;
    @JsonProperty("SF_SPONS_FOREIGN_ADDRESS2") public String sfSponsorForeignAddress2;
    @JsonProperty("SF_SPONS_FOREIGN_CITY") public String sfSponsorForeignCity;
    @JsonProperty("SF_SPONS_FOREIGN_PROV_STATE") public String sfSponsorForeignProvinceOrState;
    @JsonProperty("SF_SPONS_FOREIGN_CNTRY") public String sfSponsorForeignCountry;
    @JsonProperty("SF_SPONS_FOREIGN_POSTAL_CD") public String sfSponsorForeignPostalCD;
    @JsonProperty("SF_SPONS_EIN") public String sfSponsorEIN;

    // 31 - 40
    @JsonProperty("SF_SPONS_PHONE_NUM") public String sfSponsorPhoneNumber;
    @JsonProperty("SF_BUSINESS_CODE") public String sfBusinessCode;
    @JsonProperty("SF_ADMIN_NAME") public String sfAdminName;
    @JsonProperty("SF_ADMIN_CARE_OF_NAME") public String sfAdminCareOfName;
    @JsonProperty("SF_ADMIN_US_ADDRESS1") public String sfAdminUSAddress1;
    @JsonProperty("SF_ADMIN_US_ADDRESS2") public String sfAdminUSAddress2;
    @JsonProperty("SF_ADMIN_US_CITY") public String sfAdminUSCity;
    @JsonProperty("SF_ADMIN_US_STATE") public String sfAdminUSState;
    @JsonProperty("SF_ADMIN_US_ZIP") public String sfAdminUSZip;
    @JsonProperty("SF_ADMIN_FOREIGN_ADDRESS1") public String sfAdminForeignAddress1;

    // 41-50
    @JsonProperty("SF_ADMIN_FOREIGN_ADDRESS2") public String sfAdminForeignAddress2;
    @JsonProperty("SF_ADMIN_FOREIGN_CITY") public String sfAdminForeignCity;
    @JsonProperty("SF_ADMIN_FOREIGN_PROV_STATE") public String sfAdminForeignProvinceState;
    @JsonProperty("SF_ADMIN_FOREIGN_CNTRY") public String sfAdminForeignCountry;
    @JsonProperty("SF_ADMIN_FOREIGN_POSTAL_CD") public String sfAdminForeignPostalCD;
    @JsonProperty("SF_ADMIN_EIN") public String sfAdminEin;
    @JsonProperty("SF_ADMIN_PHONE_NUM") public String sfAdminPhoneNumber;
    @JsonProperty("SF_LAST_RPT_SPONS_NAME") public String sfLastRptSponsorName;
    @JsonProperty("SF_LAST_RPT_SPONS_EIN") public String sfLastRptSponsorEIN;
    @JsonProperty("SF_LAST_RPT_PLAN_NUM") public String sfLastRptPlanNumber;

    // 51-60

    @JsonProperty("SF_TOT_PARTCP_BOY_CNT") public int sfTotalParcpBoyCount;
    @JsonProperty("SF_TOT_ACT_RTD_SEP_BENEF_CNT") public int sfTotalAccountRtdSepBenefCount;
    @JsonProperty("SF_PARTCP_ACCOUNT_BAL_CNT") public int sfPartcpAccountBalanceCount;
    @JsonProperty("SF_ELIGIBLE_ASSETS_IND") public String sfEligibleAssetsInd;
    @JsonProperty("SF_IQPA_WAIVER_IND") public String sfIQPAWaiverInd;
    @JsonProperty("SF_TOT_ASSETS_BOY_AMT") public int sfTotalAssetsBoyAmount;
    @JsonProperty("SF_TOT_LIABILITIES_BOY_AMT") public int sfTotalLiabilitiesBoyAmount;
    @JsonProperty("SF_NET_ASSETS_BOY_AMT") public int sfNetAssetsBoyAmt;
    @JsonProperty("SF_TOT_ASSETS_EOY_AMT") public int sfTotAssetsEoyAmt;
    @JsonProperty("SF_TOT_LIABILITIES_EOY_AMT") public int sfTotalLiabilitiesEOYAmount;

    // 61-70
    @JsonProperty("SF_NET_ASSETS_EOY_AMT") public int sfNetAssetsEoyAmount;
    @JsonProperty("SF_EMPLR_CONTRIB_INCOME_AMT") public int sfEmployerContribIncomeAmount;
    @JsonProperty("SF_PARTICIP_CONTRIB_INCOME_AMT") public int sfParticipContribIncomeAmount;
    @JsonProperty("SF_OTH_CONTRIB_RCVD_AMT") public int sfOtherContribReceivedAmount;
    @JsonProperty("SF_OTHER_INCOME_AMT") public int sfOtherIncomeAmount;
    @JsonProperty("SF_TOT_INCOME_AMT") public int sfTotalIncomeAmount;
    @JsonProperty("SF_TOT_DISTRIB_BNFT_AMT") public int sftotalDistribuBenefitAmount;
    @JsonProperty("SF_CORRECTIVE_DEEMED_DISTR_AMT") public int sfCorrectiveDeemedDistrAmount;
    @JsonProperty("SF_ADMIN_SRVC_PROVIDERS_AMT") public int sfAdminSrvcProvidersAmount;
    @JsonProperty("SF_OTH_EXPENSES_AMT") public int sfOtherExpensesAmount;

    // 71-80
    @JsonProperty("SF_TOT_EXPENSES_AMT") public int sfTotalExpensesAmount;
    @JsonProperty("SF_NET_INCOME_AMT") public int sfNetIncomeAmount;
    @JsonProperty("SF_TOT_PLAN_TRANSFERS_AMT") public int sfTotalPlanTransfersAmount;
    @JsonProperty("SF_TYPE_PENSION_BNFT_CODE") public String sfTypePensionBenefitCode;
    @JsonProperty("SF_TYPE_WELFARE_BNFT_CODE") public String sfTypeWelfareBenefitCode;
    @JsonProperty("SF_FAIL_TRANSMIT_CONTRIB_IND") public String sfFailTransmitContribInd;
    @JsonProperty("SF_FAIL_TRANSMIT_CONTRIB_AMT") public int sfFailTransmitContribAmount;
    @JsonProperty("SF_PARTY_IN_INT_NOT_RPTD_IND") public String sfPartyInIntNotRptdInd;
    @JsonProperty("SF_PARTY_IN_INT_NOT_RPTD_AMT") public int sfPartyInIntNotRptdAmount;
    @JsonProperty("SF_PLAN_INS_FDLTY_BOND_IND") public String sfPanInsFidelityBondInd;

    // 81-90
    @JsonProperty("SF_PLAN_INS_FDLTY_BOND_AMT") public long sfPlanInsFidelityBondAmount;
    @JsonProperty("SF_LOSS_DISCV_DUR_YEAR_IND") public String sfLossDiscvDuringYearInd;
    @JsonProperty("SF_LOSS_DISCV_DUR_YEAR_AMT") public int sfLossDiscvDuringYearAmount;
    @JsonProperty("SF_BROKER_FEES_PAID_IND") public String sfBrokerFeesPaidInd;
    @JsonProperty("SF_BROKER_FEES_PAID_AMT") public int sfBrokerFeesPaidAmount;
    @JsonProperty("SF_FAIL_PROVIDE_BENEF_DUE_IND") public String sfFailProvideBenefitDueInd;
    @JsonProperty("SF_FAIL_PROVIDE_BENEF_DUE_AMT") public int sfFailProvideBenefitDueAmount;
    @JsonProperty("SF_PARTCP_LOANS_IND") public String sfPartcpLoansInd;
    @JsonProperty("SF_PARTCP_LOANS_EOY_AMT") public int sfPartcpLoansEOYAmount;
    @JsonProperty("SF_PLAN_BLACKOUT_PERIOD_IND") public String sfPlanBlackoutPeriodInd;

    // 91-100
    @JsonProperty("SF_COMPLY_BLACKOUT_NOTICE_IND") public String sfComplyBlackoutNoticeInd;
    @JsonProperty("SF_DB_PLAN_FUNDING_REQD_IND") public String sfDBPlanFundingRequiredInd;
    @JsonProperty("SF_DC_PLAN_FUNDING_REQD_IND") public String sfDCPlanFundingRequiredInd;
    @JsonProperty("SF_RULING_LETTER_GRANT_DATE") public String sfRulingLetterGrantDate;
    @JsonProperty("SF_SEC_412_REQ_CONTRIB_AMT") public int sfSec412RequiredContribAmount;
    @JsonProperty("SF_EMPLR_CONTRIB_PAID_AMT") public int sfEmployerContribPaidAmount;
    @JsonProperty("SF_FUNDING_DEFICIENCY_AMT") public int sfFundingDeficiencyAmount;
    @JsonProperty("SF_FUNDING_DEADLINE_IND") public String sfFundingDeadlineInd;
    @JsonProperty("SF_RES_TERM_PLAN_ADPT_IND") public String sfResTermPlanAdptInd;
    @JsonProperty("SF_RES_TERM_PLAN_ADPT_AMT") public int sfResTermPlanAdptAmount;

    // 100-109
    @JsonProperty("SF_ALL_PLAN_AST_DISTRIB_IND") public String sfAllPlanAstDistribInd;
    @JsonProperty("SF_ADMIN_SIGNED_DATE") public String sfAdminSignedDate;
    @JsonProperty("SF_ADMIN_SIGNED_NAME") public String sfAdminSignedName;
    @JsonProperty("SF_SPONS_SIGNED_DATE") public String sfSponsorSignedDate;
    @JsonProperty("SF_SPONS_SIGNED_NAME") public String sfSponsorSignedName;
    @JsonProperty("FILING_STATUS") public String filingStatus;
    @JsonProperty("DATE_RECEIVED") public String dateReceived;
    @JsonProperty("VALID_ADMIN_SIGNATURE") public String validAdminSignature;
    @JsonProperty("VALID_SPONSOR_SIGNATURE") public String validSponsorSignature;
}