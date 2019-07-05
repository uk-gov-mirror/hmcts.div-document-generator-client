provider "azurerm" {
  version = "1.21.0"
}

locals {
  aseName = "core-compute-${var.env}"
  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"

  evidence_management_client_api_url = "http://${var.evidence_management_client_api_url_part}-${local.local_env}.service.core-compute-${local.local_env}.internal"
  pdf_service_url                    = "http://${var.pdf_service_url_part}-${local.local_env}.service.core-compute-${local.local_env}.internal"
  idam_s2s_url                       = "http://${var.idam_s2s_url_prefix}-${local.local_env}.service.core-compute-${local.local_env}.internal"

  previewVaultName = "${var.reform_team}-aat"
  nonPreviewVaultName = "${var.reform_team}-${var.env}"
  vaultName = "${var.env == "preview" ? local.previewVaultName : local.nonPreviewVaultName}"
  vaultUri = "${data.azurerm_key_vault.div_key_vault.vault_uri}"

  docmosis_key_vault_uri = "https://${var.docmosis_key_vault_name}.vault.azure.net/"

  asp_name = "${var.env == "prod" ? "div-dgs-prod" : "${var.raw_product}-${var.env}"}"
  asp_rg = "${var.env == "prod" ? "div-dgs-prod" : "${var.raw_product}-${var.env}"}"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"
}

module "div-dgs" {
  source                          = "git@github.com:hmcts/moj-module-webapp.git?ref=master"
  product                         = "${var.product}-${var.component}"
  location                        = "${var.location}"
  env                             = "${var.env}"
  ilbIp                           = "${var.ilbIp}"
  resource_group_name             = "${azurerm_resource_group.rg.name}"
  subscription                    = "${var.subscription}"
  appinsights_instrumentation_key = "${var.appinsights_instrumentation_key}"
  capacity                        = "${var.capacity}"
  is_frontend                     = false
  common_tags                     = "${var.common_tags}"
  asp_name                        = "${local.asp_name}"
  asp_rg                          = "${local.asp_rg}"
  instance_size                   = "${var.instance_size}"

  app_settings = {
    REFORM_SERVICE_NAME                                   = "${var.reform_service_name}"
    REFORM_TEAM                                           = "${var.reform_team}"
    REFORM_ENVIRONMENT                                    = "${var.env}"
    AUTH_PROVIDER_SERVICE_CLIENT_BASEURL                  = "${local.idam_s2s_url}"
    AUTH_PROVIDER_SERVICE_CLIENT_MICROSERVICE             = "${var.auth_provider_service_client_microservice}"
    AUTH_PROVIDER_SERVICE_CLIENT_KEY                      = "${data.azurerm_key_vault_secret.div-doc-s2s-auth-secret.value}"
    AUTH_PROVIDER_SERVICE_CLIENT_TOKENTIMETOLIVEINSECONDS = "${var.auth_provider_service_client_tokentimetoliveinseconds}"
    PDF_SERVICE_BASEURL                                   = "${local.pdf_service_url}"
    EVIDENCE_MANAGEMENT_CLIENT_API_BASEURL                = "${local.evidence_management_client_api_url}"
    EVIDENCE_MANAGEMENT_CLIENT_API_HEALTH_ENDPOINT        = "${var.evidence_management_client_api_health_endpoint}"
    AUTH_IDAM_CLIENT_SECRET                               = "${data.azurerm_key_vault_secret.idam-secret.value}"
    DOCMOSIS_SERVICE_ACCESS_KEY                           = "${data.azurerm_key_vault_secret.docmosis_api_key.value}"
    DOCMOSIS_SERVICE_BASE_URL                             = "${data.azurerm_key_vault_secret.docmosis_endpoint.value}"
    MANAGEMENT_ENDPOINT_HEALTH_CACHE_TIMETOLIVE           = "${var.health_check_ttl}"
    FEATURE_RESP_SOLICITOR_DETAILS                        = "${var.feature_resp_solicitor_details}"
  }
}

data "azurerm_key_vault" "div_key_vault" {
    name                = "${local.vaultName}"
    resource_group_name = "${local.vaultName}"
}

data "azurerm_key_vault_secret" "div-doc-s2s-auth-secret" {
    name      = "div-doc-s2s-auth-secret"
    vault_uri = "${data.azurerm_key_vault.div_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam-secret" {
    name      = "idam-secret"
    vault_uri = "${data.azurerm_key_vault.div_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "docmosis_api_key" {
  name      = "docmosis-api-key"
  vault_uri = "${local.docmosis_key_vault_uri}"
}

data "azurerm_key_vault_secret" "docmosis_endpoint" {
  name      = "docmosis-endpoint"
  vault_uri = "${local.docmosis_key_vault_uri}"
}
