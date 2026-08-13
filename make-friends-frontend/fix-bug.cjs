const fs = require("fs");
const path = "E:\\TraePorject\\make-friends\\make-friends-frontend\\src\\views\\home\\HomeView.vue";
let content = fs.readFileSync(path, "utf8");

const oldCode = `  try {
    const params = {
      page: page.value,
      pageSize,
      gender: filter.gender || undefined,
      city: filter.city || undefined,
      ageRange: filter.ageRange || undefined
    }
    const hasFilter = filter.gender || filter.city || filter.ageRange
    const api = hasFilter ? searchUsers : getRecommendUsers
    const res = await api(params)`;

const newCode = `  try {
    const params = {
      page: page.value,
      size: pageSize,
      gender: filter.gender || undefined,
      city: filter.city || undefined
    }
    if (filter.ageRange) {
      const [minStr, maxStr] = String(filter.ageRange).split('-')
      const min = parseInt(minStr, 10)
      const max = parseInt(maxStr, 10)
      if (!isNaN(min)) params.minAge = min
      if (!isNaN(max)) params.maxAge = max
    }
    const res = await getRecommendUsers(params)`;

if (content.includes(oldCode)) {
  content = content.replace(oldCode, newCode);
  fs.writeFileSync(path, content, "utf8");
  console.log("SUCCESS: File updated successfully");
} else {
  console.log("ERROR: Old code pattern not found");
  const lines = content.split("\n");
  for (let i = 110; i < 135 && i < lines.length; i++) {
    console.log((i+1) + ": " + lines[i]);
  }
}
