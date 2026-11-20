"""Writes the models of every variant of the scene copies: each is one of the Blockbench models in
models/diegesis/<furniture>/ with the textures Refurbished gives that variant (its wood, colour or finish).

    python art/variants.py
"""
import json
import pathlib

MODELS = pathlib.Path(__file__).resolve().parent.parent / 'common/src/main/resources/assets/handson_furniture/models/diegesis'

KITCHEN = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'crimson', 'warped', 'white', 'orange',
           'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red',
           'black']

# furniture: (variants, Refurbished's texture name after the variant, the parts)
FURNITURE = {
    'fridge': (['light', 'dark'], 'fridge', ['body', 'door']),
    'freezer': (['light', 'dark'], 'fridge', ['body', 'door', 'power', 'power_plate']),
    'kitchen_cabinet': (KITCHEN, 'kitchen_storage_cabinet', ['body_left', 'body_right', 'door_left', 'door_right']),
    'kitchen_drawer': (KITCHEN, 'kitchen_drawer', ['body', 'tray', 'tray_lower']),
    'cooler': (KITCHEN[10:], 'cooler', ['body', 'lid']),
    'storage_cabinet': (KITCHEN[:10], 'storage_cabinet', ['body_left', 'body_right', 'door_left', 'door_right']),
    'drawer': (KITCHEN[:10], 'drawer', ['body', 'body_left', 'body_middle', 'body_right', 'tray', 'tray_lower']),
    'microwave': (['light', 'dark'], 'microwave', ['body', 'door', 'power']),
    'stove': (['light', 'dark'], 'stove', ['body', 'door', 'power']),
    'generator': (['light', 'dark'], 'electricity_generator', ['body_off', 'body_on', 'panel', 'power']),
    'mail_box': (KITCHEN[:10], 'mail_box', ['body', 'body_unchecked', 'door']),
}

# Parts that are another piece's model in this piece's finish: the generator's power key is the microwave's.
BORROWED = {('generator', 'power'): 'microwave/power'}

# Parts drawn in a sheet of ours, one per finish, instead of Refurbished's sheet of the variant: the appliances' controls.
OWN_SHEETS = {'power': 'handson_furniture:block/{variant}_appliance_controls',
              'power_plate': 'handson_furniture:block/{variant}_appliance_controls'}

# Parts that are the same on every variant (ours, in a texture of their own): kept as they are, no variant models.
SHARED = {'fridge': ['light'], 'microwave': ['plate', 'lamp', 'light'], 'stove': ['element', 'light'], 'generator': ['embers']}


def main():
    written = 0
    for furniture, (variants, texture, parts) in FURNITURE.items():
        folder = MODELS / furniture
        for old in folder.glob('*.json'):
            if old.stem not in parts and old.stem not in SHARED.get(furniture, []) and (furniture, old.stem) not in BORROWED:
                old.unlink()
        for variant in variants:
            for part in parts:
                model = {
                    'parent': f'handson_furniture:diegesis/{BORROWED.get((furniture, part), f"{furniture}/{part}")}',
                    'textures': {
                        'particle': f'refurbished_furniture:block/{variant}_particle',
                        'texture': OWN_SHEETS[part].format(variant=variant) if part in OWN_SHEETS
                        else f'refurbished_furniture:block/{variant}_{texture}',
                    },
                }
                (folder / f'{variant}_{part}.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8', newline='\n')
                written += 1
    print(f'{written} variant models')


if __name__ == '__main__':
    main()
