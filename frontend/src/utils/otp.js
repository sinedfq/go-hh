export const OTP_LENGTH=4
export const otpDigits=value=>String(value).replace(/[^0-9]/g,'').slice(0,OTP_LENGTH)

export function otpRetryAfter(value){const seconds=Number(value);return Number.isFinite(seconds)&&seconds>0?Math.min(3600,Math.ceil(seconds)):60}
