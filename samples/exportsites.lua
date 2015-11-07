-- On map load writes information about the loaded region to gamelog.txt
-- By Kurik Amudnil and Warmist (http://www.bay12forums.com/smf/index.php?topic=91166.msg4467072#msg4467072)

local function write_gamelog(msg)
    local log = io.open('sites.txt', 'a')
    log:write(msg.."\n")
    log:close()
end

local function fullname(item)
    return dfhack.TranslateName(item.name)..' ('..dfhack.TranslateName(item.name ,true)..')'
end

function tablelength(T)
  local count = 0
  for _ in pairs(T) do count = count + 1 end
  return count
end

local args = {...}

print('Sites '..NEWLINE)

local i = 1
local num = 0
local site = df.world_site.find(i)

local sized = false
sized = site.global_min_x ~= site.global_max_x
local message = i..''

--local details = df.world_data.rivers

--print(details)

--[[
print(fullname(site)..' '..i)
print(sized)
print(site.buildings[2].unk3)
print(site.pos.x..','..site.pos.y)
print(site.global_min_x..','..site.global_min_y..'->'..site.global_max_x..','..site.global_max_y)
if sized then
	message = message..':'..site.global_min_x..','..site.global_min_y..'->'..site.global_max_x..','..site.global_max_y
end
print(message)
--]]

----[[
while not df.isnull(site) do
	-- site positions
	-- site  .pos.x  .pos.y
	-- site  .rgn_min_x  .rgn_min_y  .rgn_max_x  .rgn_max.y
	-- site  .global_min_x  .global_min_y  .global_max_x  .global_max_y
	--site.name
	sized = site.global_min_x ~= site.global_max_x
	sized = true
	
	if sized then
		num = num + 1
		message = i..':'..site.global_min_x..','..site.global_min_y..'->'..site.global_max_x..','..site.global_max_y
		write_gamelog(message)
	end
	
	i = i + 1
	site = df.world_site.find(i)
end --]]

print(num.." Sized sites")


