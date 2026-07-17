export const supportedPlanningCities = ['杭州', '北京', '成都'];

export function supportsPlanning(city) {
  return supportedPlanningCities.includes(String(city || '').trim().replace(/市$/, ''));
}
