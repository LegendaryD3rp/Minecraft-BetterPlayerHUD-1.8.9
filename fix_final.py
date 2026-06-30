with open('src/main/java/com/yourname/betterplayerhud/BlockOutlineHandler.java') as f:
    lines = f.readlines()

targets = []
for i, line in enumerate(lines):
    s = line.strip()
    if 'drawBoundingBox(WorldRenderer' in s:
        targets.append(('drawBoundingBox', i))
    if 'drawFaceEdges(WorldRenderer' in s:
        targets.append(('drawFaceEdges', i))
    if 'drawCompleteBoxEdges(WorldRenderer' in s:
        targets.append(('drawCompleteBoxEdges', i))

ranges = []
for name, sig_idx in targets:
    # Walk back to find comment start
    j = sig_idx
    while j > 0:
        prev = lines[j-1].strip()
        if prev.startswith('/**'):
            j -= 1
            break
        elif prev == '' or prev.startswith('*') or prev == '*/' or prev.startswith('/*'):
            j -= 1
        else:
            break
    start = j

    # Walk forward balancing braces, starting from the method signature
    depth = 0
    for k in range(sig_idx, len(lines)):
        depth += lines[k].count('{') - lines[k].count('}')
        if depth <= 0 and k > sig_idx:
            end = k + 1
            break
    else:
        end = len(lines)

    ranges.append((start, end, name))
    print('{}: lines {}-{}'.format(name, start+1, end))

ranges.sort(reverse=True)
for start, end, name in ranges:
    print('Deleting {}: lines {}-{}'.format(name, start+1, end))
    del lines[start:end]

with open('src/main/java/com/yourname/betterplayerhud/BlockOutlineHandler.java', 'w') as f:
    f.writelines(lines)

print('Done. Lines remaining:', len(lines))
