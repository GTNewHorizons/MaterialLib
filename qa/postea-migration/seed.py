#!/usr/bin/env python3
"""Seeds and asserts the Postea migration gate on a pack-server world; driven by run.sh.

Subcommands (all take the world directory as the first argument):
  discover              print the material list version, the witness material's index, and the
                        materiallib item names in the world's saved id map
  seed <itemName>       write old-version witness stacks into EnderStorage, a Backpack file, an
                        OpenBlocks death dump, the GT linked-input-bus table, a player tag, Baubles
                        and Cosmetic Armor files, the quest database and a quest loot group, plus an
                        EnderStorage stack of a shape-overridden pair; records expectations in
                        <world>/postea-qa-expect.json
  assert-shift          after the examples-on boot: the store advanced one version, and the witness
                        entries of the quest database, quest loot and player files were remapped and
                        stamped
  assert-idempotent     after an unchanged boot: nothing moved again
  assert-carried <epoch>
                        after a boot without MaterialLib's transformer: the quest database, quest loot
                        and player files were rewritten after <epoch> and kept their stamps
  vanilla <dstWorld>    copy the world's level.dat into an empty <dstWorld> without its FML and Forge
                        data, as a world made by vanilla Minecraft has
"""
import gzip, io, json, struct, sys, os

BLOCK_PREFIX = chr(1)
ITEM_PREFIX = chr(2)

# ---------- minimal NBT ----------

def _ws(s):
    b = s.encode('utf8'); return struct.pack('>H', len(b)) + b

def t_byte(v): return (1, struct.pack('>b', v))
def t_short(v): return (2, struct.pack('>h', v))
def t_int(v): return (3, struct.pack('>i', v))
def t_long(v): return (4, struct.pack('>q', v))
def t_float(v): return (5, struct.pack('>f', v))
def t_double(v): return (6, struct.pack('>d', v))
def t_str(v): return (8, _ws(v))

def t_list(elem_type, payloads):
    return (9, bytes([elem_type]) + struct.pack('>i', len(payloads)) + b''.join(payloads))

def t_comp(pairs):
    out = b''
    for name, (t, payload) in pairs:
        out += bytes([t]) + _ws(name) + payload
    return (10, out + b'\x00')

def nbt_file(root_pairs):
    t, payload = t_comp(root_pairs)
    return gzip.compress(bytes([t]) + _ws('') + payload)

def read_nbt(buf):
    f = io.BytesIO(buf)
    def rname():
        n = struct.unpack('>H', f.read(2))[0]; return f.read(n).decode('utf8', 'replace')
    def pay(t):
        if t == 0: return None
        if t == 1: return struct.unpack('>b', f.read(1))[0]
        if t == 2: return struct.unpack('>h', f.read(2))[0]
        if t == 3: return struct.unpack('>i', f.read(4))[0]
        if t == 4: return struct.unpack('>q', f.read(8))[0]
        if t == 5: return struct.unpack('>f', f.read(4))[0]
        if t == 6: return struct.unpack('>d', f.read(8))[0]
        if t == 7:
            n = struct.unpack('>i', f.read(4))[0]; return f.read(n)
        if t == 8:
            n = struct.unpack('>H', f.read(2))[0]; return f.read(n).decode('utf8', 'replace')
        if t == 9:
            et = f.read(1)[0]; n = struct.unpack('>i', f.read(4))[0]
            return [pay(et) for _ in range(n)]
        if t == 10:
            out = {}
            while True:
                ct = f.read(1)[0]
                if ct == 0: return out
                key = rname()
                out[key] = pay(ct)
        if t == 11:
            n = struct.unpack('>i', f.read(4))[0]
            return list(struct.unpack('>%di' % n, f.read(4 * n)))
        raise ValueError('tag %d' % t)
    t = f.read(1)[0]; rname()
    return pay(t)

# ---------- helpers ----------

WITNESS = os.environ.get('POSTEA_QA_MATERIAL', 'Tin')
# A material whose witness-shape pair MaterialLib serves through a shape override; its seeded stack must migrate
# into the overriding item. PosteaMigrationGameTests reads it with -Dmateriallib.qa.overrideMaterial, and the item
# with -Dmateriallib.qa.overrideItem.
OVERRIDE_MATERIAL = os.environ.get('POSTEA_QA_OVERRIDE_MATERIAL', 'Iron')
UUID = '11111111-2222-3333-4444-555555555555'
# The player whose Baubles and Cosmetic Armor files PosteaMigrationGameTests loads.
FILES_PLAYER, FILES_UUID = 'qa-postea-files', '11111111-2222-3333-4444-666666666666'
# A stamp for a transformer that is never registered; PosteaMigrationGameTests requires saves to carry it.
ABSENT_STAMP = ('postea_qa:absent', 7)
STAMP_KEY = 'materiallib:idList'

def expect_file(world):
    return os.path.join(world, 'postea-qa-expect.json')

def material_store(world):
    with open(os.path.join(world, 'materiallib', 'material-ids.json'), encoding='utf8') as fh:
        return json.load(fh)

def item_id(world, name):
    data = read_nbt(gzip.decompress(open(os.path.join(world, 'level.dat'), 'rb').read()))
    for entry in data['FML']['ItemData']:
        if entry['K'] == ITEM_PREFIX + name:
            return entry['V']
    raise SystemExit('item %s not in the world id map' % name)

def stack(item, damage, slot_tag):
    pairs = [slot_tag, ('Count', t_byte(1)), ('Damage', t_short(damage))]
    if isinstance(item, str):
        pairs.insert(1, ('id', t_str(item)))
    elif item < 32000:
        pairs.insert(1, ('id', t_short(item)))
    else:
        pairs.insert(1, ('id', t_short(0)))
        pairs.insert(2, ('idExt', t_int(item)))
    return t_comp(pairs)

def quest_db(world):
    return os.path.join(world, 'betterquesting', 'QuestDatabase.json')

def quest_loot(world):
    return os.path.join(world, 'QuestLoot.json')

def player_files(world):
    playerdata = os.path.join(world, 'playerdata')
    return {'baubles': (os.path.join(playerdata, FILES_PLAYER + '.baub'), 'Baubles.Inventory'),
            'cosarmor': (os.path.join(playerdata, FILES_UUID + '.cosarmor'), 'CosArmor.Inventory')}

def read_json(path):
    with open(path, encoding='utf8') as fh:
        return json.load(fh)

def json_stamp(data):
    return data.get('POSTEA_VERSIONS:10', {}).get(STAMP_KEY + ':3')

def player_file_state(path, list_key):
    # The (id, damage) of the file's slot 0 stack and its material list stamp.
    data = read_nbt(gzip.decompress(open(path, 'rb').read()))
    slot = data[list_key][0]
    return (slot['id'], slot['Damage']), data.get('POSTEA_VERSIONS', {}).get(STAMP_KEY)

def find_seed_entry(db):
    # The first requiredItems slot of any quest, which the seeded witness replaces.
    for quest in db.get('questDatabase:9', {}).values():
        if not isinstance(quest, dict): continue
        for task in quest.get('tasks:9', {}).values():
            if not isinstance(task, dict): continue
            req = task.get('requiredItems:9')
            if isinstance(req, dict) and '0:10' in req:
                return req
    raise SystemExit('no requiredItems entry found in the quest database')

def witness_damages(db, item):
    # Every stack of the witness item anywhere in the quest database. Quest order is not stable, since dreamcraft
    # reimports the default database whenever it decides the modpack changed.
    return json_witness_damages(db.get('questDatabase:9', {}), item)

def json_witness_damages(node, item):
    out = []
    def walk(node):
        if isinstance(node, dict):
            if node.get('id:8') == item and 'Damage:2' in node:
                out.append(node['Damage:2'])
            for v in node.values():
                walk(v)
    walk(node)
    return out

# ---------- subcommands ----------

def discover(world):
    store = material_store(world)
    print('listVersion', store['listVersion'])
    print(WITNESS, 'index', store['materials'][WITNESS])
    data = read_nbt(gzip.decompress(open(os.path.join(world, 'level.dat'), 'rb').read()))
    for entry in data['FML']['ItemData']:
        if entry['K'].startswith(ITEM_PREFIX) and 'materiallib' in entry['K']:
            print(entry['K'][1:], entry['V'])

def seed(world, item_name):
    store = material_store(world)
    if store['listVersion'] != 1:
        raise SystemExit('expected a fresh v1 store, found listVersion %d' % store['listVersion'])
    old_index = store['materials'][WITNESS]
    numeric = item_id(world, item_name)

    es_dir = os.path.join(world, 'EnderStorage')
    os.makedirs(es_dir, exist_ok=True)
    open(os.path.join(es_dir, 'lock.dat'), 'wb').write(b'\x00')
    override_index = store['materials'][OVERRIDE_MATERIAL]
    chest = t_comp([
        ('Items', t_list(10, [
            stack(numeric, old_index, ('Slot', t_short(0)))[1],
            stack(numeric, override_index, ('Slot', t_short(1)))[1]])),
        ('size', t_byte(1))])
    open(os.path.join(es_dir, 'data1.dat'), 'wb').write(nbt_file([('0|global|item', chest)]))

    bp_dir = os.path.join(world, 'backpacks', 'backpacks')
    os.makedirs(bp_dir, exist_ok=True)
    open(os.path.join(bp_dir, UUID + '.dat'), 'wb').write(
        nbt_file([('Items', t_list(10, [stack(numeric, old_index, ('Slot', t_byte(0)))[1]]))]))

    dump = [
        ('Type', t_str('death')),
        ('PlayerName', t_str('qa-postea')),
        ('Created', t_long(0)),
        ('PlayerUUID', t_str(UUID)),
        ('Inventory', t_comp([
            ('size', t_int(1)),
            ('Items', t_list(10, [stack(item_name, old_index, ('Slot', t_byte(0)))[1]]))]))]
    open(os.path.join(world, 'data', 'inventory-qa-postea-death-0.dat'), 'wb').write(nbt_file(dump))

    channel = t_comp([('0', stack(numeric, old_index, ('Slot', t_byte(0)))), ('ref', t_int(1))])
    open(os.path.join(world, 'data', 'LinkedInputBusses.dat'), 'wb').write(
        nbt_file([('data', t_comp([('qa', channel)]))]))

    zeros3 = t_list(6, [t_double(0.0)[1]] * 3)
    player = [
        ('Pos', zeros3),
        ('Motion', zeros3),
        ('Rotation', t_list(5, [t_float(0.0)[1]] * 2)),
        ('Inventory', t_list(10, [stack(numeric, old_index, ('Slot', t_byte(0)))[1]])),
        ('POSTEA_VERSIONS', t_comp([(ABSENT_STAMP[0], t_int(ABSENT_STAMP[1]))]))]
    open(os.path.join(world, 'qa-postea-player.dat'), 'wb').write(nbt_file(player))

    files = player_files(world)
    os.makedirs(os.path.dirname(files['baubles'][0]), exist_ok=True)
    open(files['baubles'][0], 'wb').write(
        nbt_file([('Baubles.Inventory', t_list(10, [stack(numeric, old_index, ('Slot', t_byte(0)))[1]]))]))
    open(files['cosarmor'][0], 'wb').write(nbt_file([
        ('CosArmor.Inventory.Size', t_int(4)),
        ('CosArmor.Inventory', t_list(10, [stack(numeric, old_index, ('Slot', t_byte(0)))[1]]))]))

    loot = read_json(quest_loot(world)) if os.path.isfile(quest_loot(world)) else {}
    reward = {'ID:3': 0, 'weight:3': 1,
              'items:9': {'0:10': {'Damage:2': old_index, 'Count:3': 1, 'id:8': item_name, 'OreDict:8': ''}}}
    loot['groups:9'] = {'0:10': {'ID:3': 0, 'name:8': 'qa-postea', 'weight:3': 1, 'rewards:9': {'0:10': reward}}}
    with open(quest_loot(world), 'w', encoding='utf8') as fh:
        json.dump(loot, fh, indent=2)

    with open(quest_db(world), encoding='utf8') as fh:
        db = json.load(fh)
    req = find_seed_entry(db)
    req['0:10'] = {'Damage:2': old_index, 'Count:3': 1, 'id:8': item_name, 'OreDict:8': ''}
    with open(quest_db(world), 'w', encoding='utf8') as fh:
        json.dump(db, fh, indent='	')
    json.dump(
        {'item': item_name, 'numericId': numeric, 'oldIndex': old_index,
         'before': sorted(witness_damages(db, item_name))},
        open(expect_file(world), 'w'),
        indent=2)
    print('seeded: %s (%s) id=%d oldIndex=%d; quest and loot witness entries planted'
          % (WITNESS, item_name, numeric, old_index))

def assert_shift(world):
    exp = json.load(open(expect_file(world)))
    store = material_store(world)
    assert store['listVersion'] == 2, 'listVersion %d, expected 2' % store['listVersion']
    new_index = store['materials'][WITNESS]
    assert new_index != exp['oldIndex'], 'witness index did not move; pick a later-sorting material'
    trans = os.path.join(world, 'materiallib', 'transitions', 'v1-to-v2.json')
    assert os.path.isfile(trans), 'transition v1-to-v2.json missing'
    db = json.load(open(quest_db(world), encoding='utf8'))
    damages = witness_damages(db, exp['item'])
    assert damages, 'no %s entries in the quest database' % exp['item']
    assert new_index in damages, \
        'no quest entry holds the witness at its new index %d (old %d); found %r' \
        % (new_index, exp['oldIndex'], sorted(set(damages))[:10])
    # The pack's own quest entries of the witness item move too, one possibly onto the witness's old index, so every
    # entry is checked against the transition instead of the old index against absence.
    moved = json.load(open(trans))['moved']
    expected = sorted(moved.get(str(d), d) for d in exp['before'])
    assert sorted(damages) == expected, \
        'quest entries do not match their v1-to-v2 remap: %d found, %d expected' % (len(damages), len(expected))
    assert json_stamp(db) == 2, 'quest DB stamp: %r' % db.get('POSTEA_VERSIONS:10')
    exp['newIndex'] = new_index
    exp['after'] = sorted(damages)
    json.dump(exp, open(expect_file(world), 'w'), indent=2)
    assert_side_storages(world, exp)
    print('shift verified: %d quest entries remapped, witness at new index %d, DB, loot and player files stamped v2'
          % (len(damages), new_index))

def assert_side_storages(world, exp):
    # The quest loot and the player files rewritten in-session hold the witness at its new index, stamped v2.
    loot = read_json(quest_loot(world))
    assert json_witness_damages(loot, exp['item']) == [exp['newIndex']], \
        'quest loot witness: %r' % json_witness_damages(loot, exp['item'])
    assert json_stamp(loot) == 2, 'quest loot stamp: %r' % loot.get('POSTEA_VERSIONS:10')
    for name, (path, list_key) in player_files(world).items():
        slot, stamp = player_file_state(path, list_key)
        assert slot == (exp['numericId'], exp['newIndex']), '%s witness: %r' % (name, slot)
        assert stamp == 2, '%s stamp: %r' % (name, stamp)

def assert_idempotent(world):
    exp = json.load(open(expect_file(world)))
    store = material_store(world)
    assert store['listVersion'] == 2, 'listVersion %d, expected 2' % store['listVersion']
    db = json.load(open(quest_db(world), encoding='utf8'))
    damages = witness_damages(db, exp['item'])
    assert sorted(damages) == exp['after'], 'quest entries moved again on the idempotence boot'
    assert json_stamp(db) == 2
    assert_side_storages(world, exp)
    print('idempotence verified: %d quest entries unchanged, witness still at %d' % (len(damages), exp['newIndex']))

def assert_carried(world, since):
    paths = [quest_db(world), quest_loot(world)] + [path for path, _ in player_files(world).values()]
    stale = [p for p in paths if os.path.getmtime(p) <= since]
    assert not stale, 'not rewritten by the boot without the transformer: %r' % stale
    assert_idempotent(world)
    print('carried stamps verified: quest DB, loot and player files rewritten without the transformer, still v2')

def vanilla(world, dst):
    # Keeps only the root's Data compound. The walker skips payloads without decoding them, so Data is copied byte
    # for byte.
    raw = gzip.decompress(open(os.path.join(world, 'level.dat'), 'rb').read())
    f = io.BytesIO(raw)
    def skip(t):
        sizes = {1: 1, 2: 2, 3: 4, 4: 8, 5: 4, 6: 8}
        if t in sizes: f.read(sizes[t])
        elif t == 7: f.read(struct.unpack('>i', f.read(4))[0])
        elif t == 8: f.read(struct.unpack('>H', f.read(2))[0])
        elif t == 9:
            et = f.read(1)[0]
            for _ in range(struct.unpack('>i', f.read(4))[0]): skip(et)
        elif t == 10:
            while True:
                ct = f.read(1)[0]
                if ct == 0: return
                f.read(struct.unpack('>H', f.read(2))[0])
                skip(ct)
        elif t == 11: f.read(4 * struct.unpack('>i', f.read(4))[0])
        else: raise ValueError('tag %d' % t)
    assert f.read(1)[0] == 10
    f.read(struct.unpack('>H', f.read(2))[0])
    kept, dropped = b'', []
    while True:
        start = f.tell()
        t = f.read(1)[0]
        if t == 0: break
        name = f.read(struct.unpack('>H', f.read(2))[0]).decode('utf8')
        skip(t)
        if name == 'Data': kept += raw[start:f.tell()]
        else: dropped.append(name)
    assert kept, 'level.dat has no Data compound'
    os.makedirs(dst)
    open(os.path.join(dst, 'level.dat'), 'wb').write(gzip.compress(b'\x0a' + _ws('') + kept + b'\x00'))
    print('vanilla world written to %s; dropped %r from level.dat' % (dst, dropped))

if __name__ == '__main__':
    cmd, world = sys.argv[1], sys.argv[2]
    if cmd == 'discover': discover(world)
    elif cmd == 'seed': seed(world, sys.argv[3])
    elif cmd == 'assert-shift': assert_shift(world)
    elif cmd == 'assert-idempotent': assert_idempotent(world)
    elif cmd == 'assert-carried': assert_carried(world, float(sys.argv[3]))
    elif cmd == 'vanilla': vanilla(world, sys.argv[3])
    else: raise SystemExit('unknown command ' + cmd)
