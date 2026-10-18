# Lab helper for the Ansible deliverable (Windows PowerShell).
#   .\lab.ps1 up                      build images, start control + a CLEAN target node, wire SSH
#   .\lab.ps1 reset                   destroy and recreate the target node (clean slate)
#   .\lab.ps1 play <log> <args...>    run ansible-playbook, writing evidence to ansible/logs/<log>
#   .\lab.ps1 down                    stop everything
param(
    [Parameter(Mandatory = $true, Position = 0)][string]$Command,
    [Parameter(Position = 1)][string]$Log,
    [Parameter(Position = 2, ValueFromRemainingArguments = $true)][string[]]$PlayArgs
)
$ErrorActionPreference = 'Continue'   # docker writes progress to stderr; 'Stop' would abort on it
Set-Location $PSScriptRoot
$compose = @('compose', '-f', 'docker-compose.yml')

function Wire-Ssh {
    docker @compose exec -T control sh -c "test -f /keys/id_ed25519 || ssh-keygen -q -t ed25519 -N '' -f /keys/id_ed25519"
    $pub = (docker @compose exec -T control cat /keys/id_ed25519.pub).Trim()
    for ($i = 0; $i -lt 30; $i++) {
        docker exec rent-node systemctl is-system-running 2>$null | Out-Null
        docker exec rent-node test -d /home/ansible/.ssh 2>$null
        if ($LASTEXITCODE -eq 0) { break }
        Start-Sleep 2
    }
    docker exec rent-node sh -c "echo '$pub' > /home/ansible/.ssh/authorized_keys && chown ansible:ansible /home/ansible/.ssh/authorized_keys && chmod 600 /home/ansible/.ssh/authorized_keys && systemctl start ssh"
}

switch ($Command) {
    'up' {
        docker @compose up -d --build
        Wire-Ssh
    }
    'reset' {
        docker @compose rm -sf node
        docker @compose up -d --build node
        Wire-Ssh
    }
    'play' {
        $logPath = "/ansible/logs/$Log"
        docker @compose exec -T -e "ANSIBLE_LOG_PATH=$logPath" control ansible-playbook @PlayArgs
        exit $LASTEXITCODE
    }
    'down' { docker @compose down }
    default { throw "Unknown command '$Command'" }
}
