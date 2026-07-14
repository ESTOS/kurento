/*
 * (C) Copyright 2019 Kurento (https://www.kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include "windows-process.hpp"

#include <windows.h>
#include <psapi.h>

// ----------------------------------------------------------------------------

static unsigned long long
fileTimeToTicks (const FILETIME &ft)
{
  ULARGE_INTEGER u;

  u.LowPart = ft.dwLowDateTime;
  u.HighPart = ft.dwHighDateTime;
  return u.QuadPart;
}

// ----------------------------------------------------------------------------

unsigned long
cpuCount ()
{
  DWORD count = GetActiveProcessorCount (ALL_PROCESSOR_GROUPS);

  if (count > 0) {
    return (unsigned long) count;
  }

  SYSTEM_INFO info;

  GetNativeSystemInfo (&info);
  return info.dwNumberOfProcessors > 0 ? info.dwNumberOfProcessors : 1;
}

// ----------------------------------------------------------------------------

/**
 * Total amount of time that this process has been scheduled, in 100-ns units.
 *
 * Data is obtained from GetProcessTimes(). This value includes time scheduled
 * in user and kernel modes.
 */
static unsigned long
processTicks ()
{
  FILETIME create, exit, kernel, user;

  if (!GetProcessTimes (GetCurrentProcess (), &create, &exit, &kernel, &user)) {
    return 0;
  }

  return (unsigned long) (fileTimeToTicks (kernel) + fileTimeToTicks (user));
}

// ----------------------------------------------------------------------------

/**
 * Total amount of time that the system has spent, in 100-ns units.
 *
 * Data is obtained from GetSystemTimes(). This value includes time scheduled
 * on all CPUs, analogous to the aggregate "cpu" line in Linux /proc/stat.
 */
static unsigned long
systemTicks ()
{
  FILETIME idle, kernel, user;

  if (!GetSystemTimes (&idle, &kernel, &user)) {
    return 0;
  }

  return (unsigned long) (fileTimeToTicks (kernel) + fileTimeToTicks (user));
}

// ----------------------------------------------------------------------------

void
cpuPercentBegin (struct cpustat_t *cpustat)
{
  cpustat->processTicks = processTicks();
  cpustat->systemTicks = systemTicks();
}

// ----------------------------------------------------------------------------

float
cpuPercentEnd (const struct cpustat_t *cpustat)
{
  const unsigned long processTicksInc = processTicks() - cpustat->processTicks;

  // https://github.com/hishamhm/htop/blob/402e46bb82964366746b86d77eb5afa69c279539/linux/LinuxProcessList.c#L1032
  const unsigned long systemTicksInc = (systemTicks() - cpustat->systemTicks)
      / cpuCount();

  if (systemTicksInc == 0) {
    return 0.0f;
  }

  // https://github.com/hishamhm/htop/blob/402e46bb82964366746b86d77eb5afa69c279539/linux/LinuxProcessList.c#L832
  return 100.0f * processTicksInc / systemTicksInc;
}

// ----------------------------------------------------------------------------

long int
memoryUse ()
{
  PROCESS_MEMORY_COUNTERS pmc;

  pmc.cb = sizeof (pmc);

  if (!K32GetProcessMemoryInfo (GetCurrentProcess (), &pmc, sizeof (pmc))) {
    return 0;
  }

  return (long int) (pmc.WorkingSetSize / 1024);
}

// ----------------------------------------------------------------------------
