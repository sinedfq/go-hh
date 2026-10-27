import {lazy} from 'react'
import {trackStartupModule} from './startupLoading'
export const lazyScreen=load=>lazy(()=>trackStartupModule(load))
