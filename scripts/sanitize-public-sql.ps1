param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$sqlFiles = @(
    (Join-Path $ProjectRoot 'sql/ry_20260417.sql'),
    (Join-Path $ProjectRoot 'sql/jimureport.mysql5.7.create.sql')
)

# 排除小数坐标和连字符 ID，避免把报表 JSON 中的数字片段误判为手机号。
$phonePattern = '(?<![A-Za-z0-9.-])1[3-9][0-9]{9}(?![A-Za-z0-9.-])'
$emailPattern = '(?i)(?<![A-Z0-9._%+-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}(?![A-Z0-9.-])'
$shareRowPattern = '(?m)^INSERT INTO `jimu_report_share`[^\r\n]*(?:\r?\n)?'

foreach ($sqlFile in $sqlFiles)
{
    $resolvedPath = [System.IO.Path]::GetFullPath($sqlFile)
    $resolvedRoot = [System.IO.Path]::GetFullPath($ProjectRoot).TrimEnd('\') + '\'
    if (-not $resolvedPath.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase))
    {
        throw "拒绝修改项目目录以外的文件: $resolvedPath"
    }

    $content = [System.IO.File]::ReadAllText($resolvedPath, $utf8NoBom)
    $shareRows = [regex]::Matches($content, $shareRowPattern).Count
    $phones = [regex]::Matches($content, $phonePattern).Count
    $emails = [regex]::Matches($content, $emailPattern).Count

    $sanitized = [regex]::Replace($content, $shareRowPattern, '')
    $sanitized = [regex]::Replace($sanitized, $phonePattern, '00000000000')
    $sanitized = [regex]::Replace($sanitized, $emailPattern, 'demo@example.com')

    if ($sanitized -ne $content)
    {
        [System.IO.File]::WriteAllText($resolvedPath, $sanitized, $utf8NoBom)
    }

    Write-Output ([pscustomobject]@{
        File = [System.IO.Path]::GetFileName($resolvedPath)
        RemovedShareRows = $shareRows
        ReplacedPhones = $phones
        ReplacedEmails = $emails
    })
}
