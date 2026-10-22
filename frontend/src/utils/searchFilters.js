export const emptySearchFilters = () => ({ query: '', location: '', company: '', experience: '', remote: null, skills: [], hasCoordinates: false, sort: 'newest',salaryMin:0,salaryMax:0,currency:'',employment:'',schedule:'',salaryOnly:false })

export function readSearchFilters(search) {
    const params = new URLSearchParams(search)
    return {
        ...emptySearchFilters(), query: params.get('q') || '', location: params.get('location') || '',
        company: params.get('company_filter') || '', experience: params.get('experience') || '',
        remote: params.get('remote') === 'true' ? true : params.get('remote') === 'false' ? false : null,
        skills: (params.get('skills') || '').split(',').map(value => value.trim()).filter(Boolean),
        hasCoordinates: params.get('has_coordinates') === 'true',
        salaryMin:Number(params.get('salary_min'))||0,salaryMax:Number(params.get('salary_max'))||0,currency:params.get('currency')||'',employment:params.get('employment')||'',schedule:params.get('schedule')||'',salaryOnly:params.get('salary_only')==='true',
        sort: ['newest', 'oldest', 'popular', 'title'].includes(params.get('sort')) ? params.get('sort') : 'newest'
    }
}

export function searchParams(filters, forApi = false) {
    const params = new URLSearchParams()
    for(const [key,value] of [['salary_min',filters.salaryMin],['salary_max',filters.salaryMax],['currency',filters.currency],['employment',filters.employment],['schedule',filters.schedule]])if(value)params.set(key,String(value))
    if(filters.salaryOnly)params.set('salary_only','true')
    for (const [key, value] of [['q', filters.query], ['location', filters.location], [forApi ? 'company' : 'company_filter', filters.company], ['experience', filters.experience]]) {
        if (value?.trim()) params.set(key, value.trim())
    }
    if (typeof filters.remote === 'boolean') params.set('remote', String(filters.remote))
    if (filters.skills?.length) params.set('skills', filters.skills.join(','))
    if (filters.hasCoordinates) params.set('has_coordinates', 'true')
    if (filters.sort && filters.sort !== 'newest') params.set('sort', filters.sort)
    return params
}

export const searchKeys = ['q', 'location', 'company_filter', 'experience', 'remote', 'skills', 'has_coordinates', 'sort','salary_min','salary_max','currency','employment','schedule','salary_only']

export function restoreSavedFilters(filters) {
    const params=new URLSearchParams()
    for(const [key,value] of Object.entries(filters||{})) {
        if(value===null||value===''||value===undefined)continue
        params.set(key==='query'?'q':key==='company'?'company_filter':key,Array.isArray(value)?value.join(','):String(value))
    }
    return readSearchFilters(params.toString())
}
