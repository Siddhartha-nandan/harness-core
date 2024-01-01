def main(request):
    """
    Triggered from an HTTP Request.
    """
    print(request)
    jsonData = request.get_json(force=True)


def insert_data_bq(jsonData):
    year, month = jsonData["reportYear"], jsonData["reportMonth"]
    for year in range(2021,2023):
        for month in range(1,12):
            date_start = "%s-%s-01" % (year, month)
            fx_rates_from_api = BACKUP_CURRENCY_FX_RATES[date_start]
            for srcCurrency in fx_rates_from_api["usd"]:
                if srcCurrency.upper() not in CURRENCY_LIST:
                    continue
                # ensure precision of fx rates while performing operations
                try:
                    # 1 usd = x src
                    # 1 usd = y dest
                    # 1 src = (y/x) dest
                    if srcCurrency.upper() in jsonData["fx_rates_srcCcy_to_destCcy"][date_start]:
                        jsonData["fx_rates_srcCcy_to_destCcy"][date_start][srcCurrency.upper()] = \
                            fx_rates_from_api["usd"][jsonData["ccmPreferredCurrency"].lower()] / \
                            fx_rates_from_api["usd"][srcCurrency]
                except Exception as e:
                    print_(e, "WARN")
                    print_(
                        f"fxRate for {srcCurrency} to {jsonData['ccmPreferredCurrency']} was not found in API response.")

                conversion_source = "API"
                current_timestamp = datetime.datetime.utcnow()
                query = """ INSERT INTO `%s.CE_INTERNAL.currencyConversionFactorDefault`
                               (accountId,cloudServiceProvider,srcCurrency,ccmPreferredCurrency,
                               conversionFactor,month,conversion_source,current_timestamp,current_timestamp) """
                %(PROJECTID)