import * as XLSX from "xlsx";

// 导出二维数组为 xlsx 文件
export function exportAoaToExcel(aoa, fileName, sheetName = "成绩单", colWidths = []) {
  const sheet = XLSX.utils.aoa_to_sheet(aoa);
  if (colWidths.length > 0) {
    sheet["!cols"] = colWidths.map((wch) => ({ wch }));
  }

  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, sheet, sheetName);

  const buffer = XLSX.write(workbook, { bookType: "xlsx", type: "array" });
  const blob = new Blob([buffer], {
    type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  });

  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
