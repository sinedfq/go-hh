export const OTP_LENGTH=4
export const otpDigits=value=>String(value).replace(/[^0-9]/g,'').slice(0,OTP_LENGTH)
